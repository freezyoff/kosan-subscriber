package com.freezyoff.kosan.subscriber.utils;

public class JSON {

    private static final String[] quotedKeys = {"[\"","\"]","{\"","\"{","}\"","\"}",};

    private static final String[] quotedValues = {"[","]","{","{","}","}"};

    public static String cleanQuotedString(String json){

        return json

                //clean quoted literals \"
                .replace("\\\"", "\"")

                //clean object
                .replace("\"{", "{")
                .replace("}\"", "}")

                //clean array
                .replace("[\"", "[")
                .replace("\"]", "]")

                .replace("\\", "");

    }
}
