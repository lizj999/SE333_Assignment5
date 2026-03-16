package org.example.Amazon;

import org.example.Amazon.Cost.*;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Tests — tests multiple real components working together.
 * Uses a real Database (HSQLDB in-memory) + ShoppingCartAdaptor + Amazon + PriceRules.
 */
@DisplayName("Integration Tests")
public class AmazonIntegrationTest {

    private static Database database;
    private ShoppingCartAdaptor cart;
    private Amazon amazon;

    @BeforeAll
    static void initDatabase() {
        database = new Database();
    }

    @BeforeEach
    void setUp() {
        database.resetDatabase();
        cart = new ShoppingCartAdaptor(database);
    }

    @AfterAll
    static void tearDown() {
        database.close();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Specification-based tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("specification-based: empty cart returns zero total")
    void emptyCartReturnsZero() {
        amazon = new Amazon(cart, List.of(new RegularCost(), new DeliveryPrice(), new ExtraCostForElectronics()));
        assertEquals(0.0, amazon.calculate(), 0.001);
    }

    @Test
    @DisplayName("specification-based: single OTHER item — regular cost + delivery only")
    void singleOtherItemHasNoElectronicSurcharge() {
        amazon = new Amazon(cart, List.of(new RegularCost(), new DeliveryPrice(), new ExtraCostForElectronics()));

        amazon.addToCart(new Item(ItemType.OTHER, "Book", 1, 10.00));

        // RegularCost: 10.00, DeliveryPrice: 5.00 (1 item), ExtraCost: 0
        assertEquals(15.00, amazon.calculate(), 0.001);
    }

    @Test
    @DisplayName("specification-based: single ELECTRONIC item adds $7.50 surcharge")
    void singleElectronicItemIncludesSurcharge() {
        amazon = new Amazon(cart, List.of(new RegularCost(), new DeliveryPrice(), new ExtraCostForElectronics()));

        amazon.addToCart(new Item(ItemType.ELECTRONIC, "Laptop", 1, 100.00));

        // RegularCost: 100.00, DeliveryPrice: 5.00, ExtraCost: 7.50
        assertEquals(112.50, amazon.calculate(), 0.001);
    }

    @Test
    @DisplayName("specification-based: mixed cart (electronic + other) applies surcharge once")
    void mixedCartAppliesSurchargeOnce() {
        amazon = new Amazon(cart, List.of(new RegularCost(), new DeliveryPrice(), new ExtraCostForElectronics()));

        amazon.addToCart(new Item(ItemType.ELECTRONIC, "Phone", 1, 50.00));
        amazon.addToCart(new Item(ItemType.OTHER, "Case", 1, 10.00));

        // RegularCost: 60.00, DeliveryPrice: 5.00 (2 items), ExtraCost: 7.50
        assertEquals(72.50, amazon.calculate(), 0.001);
    }

    @Test
    @DisplayName("specification-based: 4 items triggers medium delivery price ($12.50)")
    void fourItemsTriggersMediumDelivery() {
        amazon = new Amazon(cart, List.of(new RegularCost(), new DeliveryPrice()));

        amazon.addToCart(new Item(ItemType.OTHER, "A", 1, 5.00));
        amazon.addToCart(new Item(ItemType.OTHER, "B", 1, 5.00));
        amazon.addToCart(new Item(ItemType.OTHER, "C", 1, 5.00));
        amazon.addToCart(new Item(ItemType.OTHER, "D", 1, 5.00));

        // RegularCost: 20.00, DeliveryPrice: 12.50
        assertEquals(32.50, amazon.calculate(), 0.001);
    }

    @Test
    @DisplayName("specification-based: quantity multiplier applied correctly in regular cost")
    void quantityMultiplierAppliedCorrectly() {
        amazon = new Amazon(cart, List.of(new RegularCost()));

        amazon.addToCart(new Item(ItemType.OTHER, "Pen", 3, 2.00));

        // RegularCost: 3 * 2.00 = 6.00
        assertEquals(6.00, amazon.calculate(), 0.001);
    }

    @Test
    @DisplayName("specification-based: database persists items added to cart")
    void databasePersistsItems() {
        amazon = new Amazon(cart, List.of(new RegularCost()));

        amazon.addToCart(new Item(ItemType.OTHER, "Notebook", 2, 5.00));

        List<Item> items = cart.getItems();
        assertEquals(1, items.size());
        assertEquals("Notebook", items.get(0).getName());
        assertEquals(2, items.get(0).getQuantity());
        assertEquals(5.00, items.get(0).getPricePerUnit(), 0.001);
    }

    @Test
    @DisplayName("specification-based: resetDatabase clears all items before each test")
    void resetDatabaseClearsItems() {
        cart.add(new Item(ItemType.OTHER, "Temp", 1, 1.00));
        database.resetDatabase();

        ShoppingCartAdaptor freshCart = new ShoppingCartAdaptor(database);
        assertEquals(0, freshCart.getItems().size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Structural-based tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("structural-based: all three price rules are summed in calculate()")
    void allThreeRulesAreSummed() {
        amazon = new Amazon(cart, List.of(new RegularCost(), new DeliveryPrice(), new ExtraCostForElectronics()));

        amazon.addToCart(new Item(ItemType.ELECTRONIC, "TV", 1, 200.00));

        // RegularCost: 200.00, DeliveryPrice: 5.00, ExtraCost: 7.50
        double result = amazon.calculate();
        assertEquals(212.50, result, 0.001);
    }

    @Test
    @DisplayName("structural-based: calculate() with only RegularCost rule")
    void calculateWithOnlyRegularCost() {
        amazon = new Amazon(cart, List.of(new RegularCost()));

        amazon.addToCart(new Item(ItemType.OTHER, "Cup", 2, 3.00));
        assertEquals(6.00, amazon.calculate(), 0.001);
    }

    @Test
    @DisplayName("structural-based: 11+ items triggers max delivery price ($20.00)")
    void elevenItemsTriggersMaxDelivery() {
        amazon = new Amazon(cart, List.of(new DeliveryPrice()));

        for (int i = 0; i < 11; i++) {
            amazon.addToCart(new Item(ItemType.OTHER, "Item" + i, 1, 1.00));
        }

        assertEquals(20.00, amazon.calculate(), 0.001);
    }

    @Test
    @DisplayName("structural-based: items retrieved from DB match what was inserted")
    void itemsRetrievedMatchInserted() {
        amazon = new Amazon(cart, List.of(new RegularCost()));

        amazon.addToCart(new Item(ItemType.ELECTRONIC, "Keyboard", 1, 45.00));
        amazon.addToCart(new Item(ItemType.OTHER, "Mouse", 2, 15.00));

        List<Item> items = cart.getItems();
        assertEquals(2, items.size());

        Item keyboard = items.stream().filter(i -> i.getName().equals("Keyboard")).findFirst().orElseThrow();
        assertEquals(ItemType.ELECTRONIC, keyboard.getType());
        assertEquals(45.00, keyboard.getPricePerUnit(), 0.001);

        Item mouse = items.stream().filter(i -> i.getName().equals("Mouse")).findFirst().orElseThrow();
        assertEquals(2, mouse.getQuantity());
    }

    @Test
    @DisplayName("structural-based: only OTHER items — no electronic surcharge applied")
    void onlyOtherItemsNoSurcharge() {
        amazon = new Amazon(cart, List.of(new ExtraCostForElectronics()));

        amazon.addToCart(new Item(ItemType.OTHER, "Book", 1, 20.00));
        amazon.addToCart(new Item(ItemType.OTHER, "Pen", 1, 5.00));

        assertEquals(0.0, amazon.calculate(), 0.001);
    }
}
