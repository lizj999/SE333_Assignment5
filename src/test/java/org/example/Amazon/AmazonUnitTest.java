package org.example.Amazon;

import org.example.Amazon.Cost.*;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests — tests each class in isolation.
 * Uses Mockito mocks/stubs for all external dependencies (ShoppingCart, PriceRule, Database).
 */
@DisplayName("Unit Tests")
public class AmazonUnitTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Amazon class — specification-based
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("specification-based: calculate() returns 0 when no rules")
    void calculateReturnsZeroWithNoRules() {
        ShoppingCart mockCart = mock(ShoppingCart.class);
        when(mockCart.getItems()).thenReturn(List.of());

        Amazon amazon = new Amazon(mockCart, List.of());
        assertEquals(0.0, amazon.calculate(), 0.001);
    }

    @Test
    @DisplayName("specification-based: calculate() sums all price rules")
    void calculateSumsAllRules() {
        ShoppingCart mockCart = mock(ShoppingCart.class);
        when(mockCart.getItems()).thenReturn(List.of());

        PriceRule rule1 = mock(PriceRule.class);
        PriceRule rule2 = mock(PriceRule.class);
        when(rule1.priceToAggregate(anyList())).thenReturn(10.0);
        when(rule2.priceToAggregate(anyList())).thenReturn(5.0);

        Amazon amazon = new Amazon(mockCart, List.of(rule1, rule2));
        assertEquals(15.0, amazon.calculate(), 0.001);
    }

    @Test
    @DisplayName("specification-based: addToCart() delegates to the cart")
    void addToCartDelegatesToCart() {
        ShoppingCart mockCart = mock(ShoppingCart.class);
        Amazon amazon = new Amazon(mockCart, List.of());

        Item item = new Item(ItemType.OTHER, "Book", 1, 10.00);
        amazon.addToCart(item);

        verify(mockCart, times(1)).add(item);
    }

    @Test
    @DisplayName("specification-based: calculate() passes cart items to each rule")
    void calculatePassesItemsToEachRule() {
        Item item = new Item(ItemType.OTHER, "Pen", 1, 2.00);
        ShoppingCart mockCart = mock(ShoppingCart.class);
        when(mockCart.getItems()).thenReturn(List.of(item));

        PriceRule mockRule = mock(PriceRule.class);
        when(mockRule.priceToAggregate(List.of(item))).thenReturn(2.0);

        Amazon amazon = new Amazon(mockCart, List.of(mockRule));
        amazon.calculate();

        verify(mockRule, times(1)).priceToAggregate(List.of(item));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Amazon class — structural-based
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("structural-based: calculate() loops over every rule once")
    void calculateLoopsOverEveryRule() {
        ShoppingCart mockCart = mock(ShoppingCart.class);
        when(mockCart.getItems()).thenReturn(List.of());

        PriceRule r1 = mock(PriceRule.class);
        PriceRule r2 = mock(PriceRule.class);
        PriceRule r3 = mock(PriceRule.class);
        when(r1.priceToAggregate(anyList())).thenReturn(1.0);
        when(r2.priceToAggregate(anyList())).thenReturn(2.0);
        when(r3.priceToAggregate(anyList())).thenReturn(3.0);

        Amazon amazon = new Amazon(mockCart, List.of(r1, r2, r3));
        double result = amazon.calculate();

        assertEquals(6.0, result, 0.001);
        verify(r1).priceToAggregate(anyList());
        verify(r2).priceToAggregate(anyList());
        verify(r3).priceToAggregate(anyList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RegularCost — specification-based
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("specification-based: RegularCost returns 0 for empty cart")
    void regularCostEmptyCart() {
        RegularCost rule = new RegularCost();
        assertEquals(0.0, rule.priceToAggregate(List.of()), 0.001);
    }

    @Test
    @DisplayName("specification-based: RegularCost multiplies price by quantity for each item")
    void regularCostMultipliesPriceByQuantity() {
        RegularCost rule = new RegularCost();
        List<Item> items = List.of(
                new Item(ItemType.OTHER, "A", 2, 5.00),
                new Item(ItemType.OTHER, "B", 3, 4.00)
        );
        // 2*5 + 3*4 = 10 + 12 = 22
        assertEquals(22.0, rule.priceToAggregate(items), 0.001);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RegularCost — structural-based
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("structural-based: RegularCost loops through all items")
    void regularCostLoopsThroughAllItems() {
        RegularCost rule = new RegularCost();
        List<Item> items = List.of(
                new Item(ItemType.OTHER, "X", 1, 10.00),
                new Item(ItemType.OTHER, "Y", 1, 20.00),
                new Item(ItemType.OTHER, "Z", 1, 30.00)
        );
        assertEquals(60.0, rule.priceToAggregate(items), 0.001);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ExtraCostForElectronics — specification-based
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("specification-based: ExtraCost returns 7.50 when cart has an ELECTRONIC item")
    void extraCostReturns750WhenElectronicPresent() {
        ExtraCostForElectronics rule = new ExtraCostForElectronics();
        List<Item> items = List.of(new Item(ItemType.ELECTRONIC, "Phone", 1, 100.00));
        assertEquals(7.50, rule.priceToAggregate(items), 0.001);
    }

    @Test
    @DisplayName("specification-based: ExtraCost returns 0 when no ELECTRONIC items")
    void extraCostReturnsZeroWithNoElectronics() {
        ExtraCostForElectronics rule = new ExtraCostForElectronics();
        List<Item> items = List.of(new Item(ItemType.OTHER, "Book", 1, 15.00));
        assertEquals(0.0, rule.priceToAggregate(items), 0.001);
    }

    @Test
    @DisplayName("specification-based: ExtraCost returns 0 for empty cart")
    void extraCostReturnsZeroForEmptyCart() {
        ExtraCostForElectronics rule = new ExtraCostForElectronics();
        assertEquals(0.0, rule.priceToAggregate(List.of()), 0.001);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ExtraCostForElectronics — structural-based
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("structural-based: ExtraCost is applied only once even with multiple ELECTRONIC items")
    void extraCostAppliedOnceForMultipleElectronics() {
        ExtraCostForElectronics rule = new ExtraCostForElectronics();
        List<Item> items = List.of(
                new Item(ItemType.ELECTRONIC, "TV", 1, 300.00),
                new Item(ItemType.ELECTRONIC, "Phone", 1, 100.00)
        );
        assertEquals(7.50, rule.priceToAggregate(items), 0.001);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DeliveryPrice — specification-based
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("specification-based: DeliveryPrice returns 0 for empty cart")
    void deliveryPriceZeroForEmptyCart() {
        DeliveryPrice rule = new DeliveryPrice();
        assertEquals(0.0, rule.priceToAggregate(List.of()), 0.001);
    }

    @Test
    @DisplayName("specification-based: DeliveryPrice returns 5.00 for 1 item")
    void deliveryPriceFiveForOneItem() {
        DeliveryPrice rule = new DeliveryPrice();
        List<Item> items = List.of(new Item(ItemType.OTHER, "A", 1, 1.00));
        assertEquals(5.0, rule.priceToAggregate(items), 0.001);
    }

    @Test
    @DisplayName("specification-based: DeliveryPrice returns 5.00 for 3 items (boundary)")
    void deliveryPriceFiveForThreeItems() {
        DeliveryPrice rule = new DeliveryPrice();
        List<Item> items = List.of(
                new Item(ItemType.OTHER, "A", 1, 1.00),
                new Item(ItemType.OTHER, "B", 1, 1.00),
                new Item(ItemType.OTHER, "C", 1, 1.00)
        );
        assertEquals(5.0, rule.priceToAggregate(items), 0.001);
    }

    @Test
    @DisplayName("specification-based: DeliveryPrice returns 12.50 for 4 items (boundary)")
    void deliveryPriceMediumForFourItems() {
        DeliveryPrice rule = new DeliveryPrice();
        List<Item> items = List.of(
                new Item(ItemType.OTHER, "A", 1, 1.00),
                new Item(ItemType.OTHER, "B", 1, 1.00),
                new Item(ItemType.OTHER, "C", 1, 1.00),
                new Item(ItemType.OTHER, "D", 1, 1.00)
        );
        assertEquals(12.5, rule.priceToAggregate(items), 0.001);
    }

    @Test
    @DisplayName("specification-based: DeliveryPrice returns 12.50 for 10 items (boundary)")
    void deliveryPriceMediumForTenItems() {
        DeliveryPrice rule = new DeliveryPrice();
        List<Item> items = java.util.Collections.nCopies(10, new Item(ItemType.OTHER, "X", 1, 1.00));
        assertEquals(12.5, rule.priceToAggregate(items), 0.001);
    }

    @Test
    @DisplayName("specification-based: DeliveryPrice returns 20.00 for 11+ items")
    void deliveryPriceMaxForElevenPlusItems() {
        DeliveryPrice rule = new DeliveryPrice();
        List<Item> items = java.util.Collections.nCopies(11, new Item(ItemType.OTHER, "X", 1, 1.00));
        assertEquals(20.0, rule.priceToAggregate(items), 0.001);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DeliveryPrice — structural-based
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("structural-based: DeliveryPrice uses cart size, not quantity field")
    void deliveryPriceUsesCartSizeNotQuantity() {
        // One item with quantity=100 should still count as 1 cart entry → $5
        DeliveryPrice rule = new DeliveryPrice();
        List<Item> items = List.of(new Item(ItemType.OTHER, "Bulk", 100, 1.00));
        assertEquals(5.0, rule.priceToAggregate(items), 0.001);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ShoppingCartAdaptor — unit tests with mocked Database
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("specification-based: ShoppingCartAdaptor.add() calls withSql on the database")
    void shoppingCartAdaptorAddCallsDatabase() {
        Database mockDb = mock(Database.class);
        // We need a real connection for add() to work — use a real DB here
        // Instead, verify the adaptor calls withSql (spy approach)
        Database spyDb = Mockito.spy(new Database());
        ShoppingCartAdaptor adaptor = new ShoppingCartAdaptor(spyDb);

        Item item = new Item(ItemType.OTHER, "Widget", 1, 9.99);
        adaptor.add(item);

        verify(spyDb, atLeastOnce()).withSql(any());
        spyDb.close();
    }

    @Test
    @DisplayName("structural-based: ShoppingCartAdaptor.getItems() returns correct ItemType from DB")
    void shoppingCartAdaptorGetItemsCorrectType() {
        Database db = new Database();
        db.resetDatabase();
        ShoppingCartAdaptor adaptor = new ShoppingCartAdaptor(db);

        adaptor.add(new Item(ItemType.ELECTRONIC, "Tablet", 1, 250.00));
        List<Item> items = adaptor.getItems();

        assertEquals(1, items.size());
        assertEquals(ItemType.ELECTRONIC, items.get(0).getType());
        db.close();
    }
}
