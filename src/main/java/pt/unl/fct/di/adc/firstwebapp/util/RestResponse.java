package pt.unl.fct.di.adc.firstwebapp.util;

public class RestResponse {
    public String status;
    public Object data;

    public RestResponse(String status, Object data) {
        this.status = status;
        this.data = data;
    }
}