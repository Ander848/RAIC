package com.checkautos;

import com.checkautos.auth.AuthenticationManager;
import com.checkautos.cars.CarManager;
import com.checkautos.cars.ValuationManager;
import com.checkautos.models.Car;
import com.checkautos.navigation.NavigationManager;
import com.checkautos.prediction.PredictionManager;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AppContext {

    private final AuthenticationManager authManager;
    private final CarManager            carManager;
    private final ValuationManager      valuationManager;
    private final NavigationManager     navigationManager;
    private final PredictionManager     predictionManager;

    public AppContext(AuthenticationManager authManager,
                      CarManager carManager,
                      ValuationManager valuationManager,
                      NavigationManager navigationManager,
                      PredictionManager predictionManager) {
        this.authManager       = authManager;
        this.carManager        = carManager;
        this.valuationManager  = valuationManager;
        this.navigationManager = navigationManager;
        this.predictionManager = predictionManager;
    }

    public AuthenticationManager getAuthManager()       { return authManager; }
    public CarManager            getCarManager()        { return carManager; }
    public ValuationManager      getValuationManager()  { return valuationManager; }
    public NavigationManager     getNavigationManager() { return navigationManager; }
    public PredictionManager     getPredictionManager() { return predictionManager; }

    public boolean login(String username, String password) {
        if (authManager.login(username, password)) {
            navigationManager.reset();
            navigationManager.switchView("dashboard");
            carManager.clearCurrentCar();
            return true;
        }
        return false;
    }

    public void logout() {
        authManager.logout();
        navigationManager.reset();
        carManager.clearCurrentCar();
    }

    public boolean createAccount(String nombre, String username, String password) {
        return authManager.createAccount(nombre, username, password);
    }

    public void startNewCarRegistration() {
        carManager.startNewCarRegistration();
        navigationManager.switchView("registro");
    }

    public Car getCurrentCar() { return carManager.getCurrentCar(); }

    public boolean saveAndPublishCar() { return carManager.saveAndRegisterCar(); }

    public List<Car> getAllCars() { return carManager.getAllCars(); }

    public boolean navigateTo(String viewId) { return navigationManager.switchView(viewId); }

    public String getCurrentView() { return navigationManager.getCurrentView(); }

    private static AppContext instance;

    public static AppContext getInstance() { return instance; }

    @jakarta.annotation.PostConstruct
    private void registerInstance() { instance = this; }

    @Override
    public String toString() {
        return "AppContext{view='" + navigationManager.getCurrentView() + "'" +
               ", totalAutos=" + carManager.getTotalCars() + '}';
    }
}