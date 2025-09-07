public class TestMainClass {
    public static void main(String[] args) {
        System.out.println("Testing if Java can find and run the main class...");

        try {
            // Try to load the main class
            Class<?> mainClass = Class.forName("com.medco.HealthConnectProvider.HealthConnectProviderApplication");
            System.out.println("SUCCESS: Main class found: " + mainClass.getName());

            // Check if it has a main method
            java.lang.reflect.Method mainMethod = mainClass.getMethod("main", String[].class);
            System.out.println("SUCCESS: Main method found: " + mainMethod.getName());

            System.out.println("RESULT: The main class is properly compiled and accessible!");
            System.out.println("NOTE: Issue is likely with application startup (database connection, etc.)");

        } catch (ClassNotFoundException e) {
            System.out.println("ERROR: Main class not found: " + e.getMessage());
            System.out.println("TIP: The class may not be compiled or in the wrong package");
        } catch (NoSuchMethodException e) {
            System.out.println("ERROR: Main method not found: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("ERROR: Other error: " + e.getMessage());
        }
    }
}
