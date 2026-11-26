package com.recipecatalog.backend;

import com.recipecatalog.backend.chat.ChefChatService;
import com.recipecatalog.backend.recipe.IngredientNormalizer;
import com.recipecatalog.backend.recipe.RecipeService;
import com.recipecatalog.backend.recipe.dto.CreateRecipeRequest;
import com.recipecatalog.backend.recipe.dto.IngredientDto;
import com.recipecatalog.backend.recipe.dto.RecipeSummaryDto;
import com.recipecatalog.backend.user.User;
import com.recipecatalog.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BackendApplicationTests {

	@Autowired
	private IngredientNormalizer normalizer;

	@Autowired
	private RecipeService recipeService;

	@Autowired
	private ChefChatService chefChatService;

	@Autowired
	private UserRepository userRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void ingredientNormalizerAutoFixesTyposAndRejectsGibberish() {
		var eggs = normalizer.normalize("egf");
		assertTrue(eggs.valid());
		assertTrue(eggs.corrected());
		assertEquals("Eggs", eggs.normalized());

		var egy = normalizer.normalize("egy");
		assertTrue(egy.valid());
		assertEquals("Eggs", egy.normalized());

		var courgette = normalizer.normalize("courgette");
		assertTrue(courgette.valid());
		assertEquals("Zucchini", courgette.normalized());

		var tomato = normalizer.normalize("tomto");
		assertTrue(tomato.valid());
		assertEquals("Tomato", tomato.normalized());

		var numeric = normalizer.normalize("12345");
		assertFalse(numeric.valid());

		var repeat = normalizer.normalize("zzzzz");
		assertFalse(repeat.valid());
	}

	@Test
	void seededCatalogHasTwelvePerCategoryAndUniqueTitlesAndImages() {
		List<RecipeSummaryDto> all = recipeService.list(null, null, null, null, null, null, null, null);
		assertTrue(all.size() >= 120, "Expected at least 120 recipes, got " + all.size());

		Map<String, Long> counts = all.stream()
				.collect(Collectors.groupingBy(RecipeSummaryDto::category, Collectors.counting()));
		for (String cat : List.of("ENTREES", "PLATS", "DESSERTS", "SWEETS", "BREAKFAST", "APERITIFS", "BOULANGE", "BOISSONS")) {
			assertTrue(counts.getOrDefault(cat, 0L) >= 12, "Category " + cat + " should have at least 12 recipes, got " + counts.getOrDefault(cat, 0L));
		}

		// Check unique normalized titles and unique image URLs across seeded recipes
		List<RecipeSummaryDto> seeded = all.stream()
				.filter(r -> "Chef Kitchenette".equals(r.authorName()))
				.toList();
		Set<String> titles = new HashSet<>();
		Set<String> images = new HashSet<>();
		for (RecipeSummaryDto r : seeded) {
			String key = RecipeService.normalizeTitleKey(r.title());
			assertTrue(titles.add(key), "Duplicate seeded title found: " + r.title());
			if (r.imageUrl() != null && !r.imageUrl().isBlank()) {
				assertTrue(images.add(r.imageUrl().trim()), "Duplicate seeded imageUrl found: " + r.imageUrl());
			}
		}

		assertTrue(all.stream().anyMatch(r -> r.topNotes() != null && !r.topNotes().isEmpty()));
	}

	@Test
	void leaderboardRanksReviewedRecipesBeforeZeroReviewsAndBestRatedFirst() {
		List<RecipeSummaryDto> ranked = recipeService.ranking();
		assertFalse(ranked.isEmpty());
		assertTrue(ranked.get(0).ratingCount() > 0, "Top #1 recipe must have reviews");
		assertTrue(ranked.get(0).averageRating() >= 4.0, "Top #1 recipe should have high average rating");

		// Verify all reviewed recipes precede 0-review recipes
		boolean seenZeroReview = false;
		for (RecipeSummaryDto r : ranked) {
			if (r.ratingCount() == 0) {
				seenZeroReview = true;
			} else if (seenZeroReview) {
				fail("Found reviewed recipe '" + r.title() + "' after a 0-review recipe in leaderboard!");
			}
		}

		List<RecipeSummaryDto> withEggs = recipeService.list(null, null, "egy", null, null, null, null, null);
		assertFalse(withEggs.isEmpty());

		List<RecipeSummaryDto> nutFree = recipeService.list(null, null, null, null, null, "nuts", null, null);
		assertTrue(nutFree.stream().noneMatch(r -> r.allergens() != null && r.allergens().contains("nuts")));
	}

	@Test
	void duplicateTitleForSameAuthorIsBlocked() {
		User u = new User();
		u.setName("Test Chef");
		u.setEmail("dupcheck-" + UUID.randomUUID() + "@example.com");
		u.setPassword("hashed");
		u = userRepository.save(u);
		var auth = new UsernamePasswordAuthenticationToken(u.getEmail(), null, List.of());

		var req = new CreateRecipeRequest(
				"Unique Test Couscous Dish",
				"A delicious test recipe for uniqueness validation.",
				null, 10, 15, 2,
				List.of(new IngredientDto("Eggs", "2"), new IngredientDto("Tomato", "2")),
				List.of("Prep the ingredients carefully.", "Cook everything until golden."),
				"PLATS", "Tasty!", "BUDGET", 8.0, "QUICK", ""
		);
		var created = recipeService.create(req, auth);
		try {
			assertThrows(ResponseStatusException.class, () -> recipeService.create(req, auth));
		} finally {
			recipeService.delete(created.id(), auth);
			userRepository.delete(u);
		}
	}

	@Test
	void mrMayoHandlesCookingAndBlocksOffTopic() {
		var dateReply = chefChatService.respond("Plan a romantic date night menu");
		assertFalse(dateReply.offTopic());
		assertFalse(dateReply.suggestions().isEmpty());

		var offTopic = chefChatService.respond("Tell me how to write python code for bitcoin");
		assertTrue(offTopic.offTopic());
		assertTrue(offTopic.suggestions().isEmpty());
	}
}
