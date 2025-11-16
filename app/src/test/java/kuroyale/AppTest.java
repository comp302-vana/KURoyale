package kuroyale;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AppTest {
    @Test void appHas() {
        App classUnderTest = new App();
        assertNotNull(classUnderTest, "mesage");
    }
}
