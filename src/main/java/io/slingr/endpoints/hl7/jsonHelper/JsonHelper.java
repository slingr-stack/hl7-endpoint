package io.slingr.endpoints.hl7.jsonHelper;

import io.slingr.endpoints.exceptions.EndpointException;
import io.slingr.endpoints.exceptions.ErrorCode;
import io.slingr.endpoints.utils.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonHelper {

    //Aux function to convert Json properties that could be array of Jsons, just one Json or values in an array(This is just to facilitate the writing of the Json from the sending app).
    public static List<Json> arrayPropertyToJson(String propPath, String propValue) {
        List<Json> paramList = new ArrayList<>();
        try {
            //We check the type of the Json by looking at the first char
            switch (propValue.trim().charAt(0)){
                //Array
                case '[':
                    //This is to get the first non space char after the [
                    Pattern p = Pattern.compile("\\[(\\s)*");
                    final Matcher matcher = p.matcher(propValue);
                    matcher.find();
                    int indexOfChar = matcher.end();

                    //We check if its an array of jsons, or an array of values
                    if(propValue.charAt(indexOfChar) == '{'){
                        paramList = Json.parse(propValue).jsons();
                    } else {
                        List<String> arrayOfValues = Json.parse("{\"values\":"+propValue+"}").strings("values");
                        for (String value: arrayOfValues) {
                            paramList.add(Json.parse("{\"mainValue\":\""+value+"\"}"));
                        }
                    }
                    break;

                //Single Json
                case '{':
                    paramList.add(Json.parse(propValue));
                    break;

                //Single value
                default:
                    paramList.add(Json.parse("{\"mainValue\":\""+propValue+"\"}"));
            }
        } catch (RuntimeException error) {
            System.out.println("El error es: "+error);
            throw EndpointException.permanent(ErrorCode.ARGUMENT,"The property ['"+propPath+"'] should be an array of JSONs, a single JSON or a simple value");

        }

        return paramList;
    }

    //Aux function to convert Json properties that could be array of Jsons or just one Json (Just to facilitate the writing of the Json from the sending app).
    public static List<Json> multipleJsonPropertyParse(String propPath, String propValue) {
        List<Json> paramList = new ArrayList<>();
        //We wrap in try catch because if the parsing fails, the property does not have te correct format
        try{
            //We check the type of the Json by looking at the first char
            switch (propValue.trim().charAt(0)){
                //Array of jsons
                case '[':
                    paramList = Json.parse(propValue).jsons();
                    break;
                //Single Json
                case '{':
                    paramList.add(Json.parse(propValue));
                    break;
                //Incorrect format
                default:
                    throw new RuntimeException();
            }
        } catch (RuntimeException error) {
            throw EndpointException.permanent(ErrorCode.ARGUMENT,"The property ['"+propPath+"'] should be an array of JSONs or a single JSON");
        }
        return paramList;
    }

    public static Json jsonOrValuePropertyParse(String propPath, String propValue) {
        Json finalJson = null;
        //We check the type of the Json by looking at the first char
        try {
            switch (propValue.trim().charAt(0)){
                case '{':
                    finalJson = Json.parse(propValue);
                    break;
                default:
                    finalJson = Json.parse("{\"mainValue\":\""+propValue+"\"}");
            }
        } catch (RuntimeException error){
            throw EndpointException.permanent(ErrorCode.ARGUMENT,"The property ['"+propPath+"'] should be a JSON or a single value");
        }
        return finalJson;
    }

    public static Json singleJsonPropertyParse(String propPath,String propValue){
        Json finalJson = null;
        try {
            finalJson = Json.parse(propValue);
        } catch (RuntimeException rError){
            throw EndpointException.permanent(ErrorCode.ARGUMENT,"The property ['"+propPath+"'] should be a JSON");
        }
        return finalJson;
    }

    public static List<String> multipleValuesPropertyParse(String propPath,String propValue){
        List<String> finalJson = null;
        try {
            finalJson = Json.parse("{\"values\":\""+propValue+"\"}").strings("values");
        } catch (RuntimeException rError){
            throw EndpointException.permanent(ErrorCode.ARGUMENT,"The property ['"+propPath+"'] should be an array of values");
        }
        return finalJson;
    }
}
