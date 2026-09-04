package lucns.oblivium.services;

public class AppStateRegister {

    private boolean state;

    private static AppStateRegister instance;

    public static AppStateRegister getInstance() {
        if (instance == null) {
            synchronized (AppStateRegister.class) {
                instance = new AppStateRegister();
            }
        }
        return instance;
    }

    protected AppStateRegister() {}

    public void setState(boolean state) {
        this.state = state;
    }

    public boolean getState() {
        return state;
    }
}
