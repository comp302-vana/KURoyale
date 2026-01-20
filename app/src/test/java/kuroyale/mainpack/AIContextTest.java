package kuroyale.mainpack;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import kuroyale.ai.AIContext;
import kuroyale.ai.SimpleAI;
import kuroyale.arenapack.ArenaMap;

/**
 * Test class for AIContext as an Abstract Data Type.
 * Tests focus on public operations and observable behavior,
 * using repOk() to verify invariants are maintained.
 */
public class AIContextTest {

    private ArenaMap arenaMap;
    private AIContext aiContext;

    // Minimal stub for GameEngine to satisfy repOk requirement
    private static class GameEngineStub extends GameEngine {
        @Override
        public void executeSpell(int spellCardID, int targetRow, int targetCol, boolean isPlayerSpell) {
            // Stub implementation - does nothing for testing
        }
    }

    @BeforeEach
    void setUp() {
        arenaMap = new ArenaMap();
        aiContext = new AIContext(arenaMap, new GameEngineStub(), new SimpleAI());
    }

    /**
     * Helper method to check repOk via reflection
     */
    private boolean checkRepOk(AIContext ai) throws Exception {
        Method repOkMethod = AIContext.class.getDeclaredMethod("repOk");
        repOkMethod.setAccessible(true);
        return (Boolean) repOkMethod.invoke(ai);
    }

    /**
     * Test Case 1: Constructor establishes valid state
     * Tests that the constructor creates a AIContext instance
     * that satisfies the representation invariant.
     * ADT Test: Verifies the creation operation maintains invariants.
     */
    @Test
    void testConstructorEstablishesValidState() throws Exception {
        // Test that constructor doesn't throw
        assertNotNull(aiContext, "Constructor should create a non-null AIContext instance");
        
        // Test that repOk holds after construction
        assertTrue(checkRepOk(aiContext), 
                   "repOk() should return true after constructor initialization");
    }

    /**
     * Test Case 2: Update maintains invariants
     * Tests that calling update() maintains the representation invariant.
     * ADT Test: Verifies the update operation preserves invariants.
     */
    @Test
    void testUpdateMaintainsInvariants() throws Exception {
        // Verify initial state is valid
        assertTrue(checkRepOk(aiContext), "Initial state should be valid");
        
        // Call update operation
        aiContext.update(1.0, 30);
        
        // Verify repOk still holds after update
        assertTrue(checkRepOk(aiContext), 
                   "repOk() should remain true after update() call");
    }

    /**
     * Test Case 3: Multiple updates maintain invariants
     * Tests that repeated calls to update() continue to maintain invariants.
     * ADT Test: Verifies operations maintain invariants over multiple calls.
     */
    @Test
    void testMultipleUpdatesMaintainInvariants() throws Exception {
        // Perform multiple update operations
        for (int i = 0; i < 2; i++) {
            aiContext.update(2.8, 30 + i);
            assertTrue(checkRepOk(aiContext), 
                      "repOk() should remain true after update " + (i + 1));
        }
    }

    /**
     * Test Case 4: Update with different time parameters maintains invariants
     * Tests that update() maintains invariants with various time inputs.
     * ADT Test: Verifies operation behavior across different inputs.
     */
    @Test
    void testUpdateWithDifferentTimesMaintainsInvariants() throws Exception {
        // Test update before 60 seconds (double elixir regen)
        aiContext.update(5.0, 30);
        assertTrue(checkRepOk(aiContext), 
                  "repOk() should hold after update before 60 seconds");
        
        // Test update after 60 seconds (normal elixir regen)
        aiContext.update(5.0, 70);
        assertTrue(checkRepOk(aiContext), 
                  "repOk() should hold after update after 60 seconds");
        
        // Test update with very small deltaTime
        aiContext.update(0.001, 50);
        assertTrue(checkRepOk(aiContext), 
                  "repOk() should hold after update with small deltaTime");
        
        // Test update with large deltaTime
        aiContext.update(100.0, 50);
        assertTrue(checkRepOk(aiContext), 
                  "repOk() should hold after update with large deltaTime");
    }
}