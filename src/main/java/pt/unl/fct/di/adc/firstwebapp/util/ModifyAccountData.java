package pt.unl.fct.di.adc.firstwebapp.util;

public class ModifyAccountData {
    public String username;
    public Attributes attributes;

    public ModifyAccountData() {}

    public static class Attributes{
        public String phone;
        public String address;
    }
}