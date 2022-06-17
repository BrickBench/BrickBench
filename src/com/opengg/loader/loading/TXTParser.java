package com.opengg.loader.loading;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public abstract class TXTParser {
    protected String currentBlock = "";
    protected String currentAttribute="";

    public void parseFile(Path txtPath) throws IOException {
        Files.lines(txtPath).forEach(line->{
            String[] tokens = Arrays.stream(line.split("[\s=,]"))
                    .filter(e -> !e.isEmpty())
                    .map(String::trim)
                    .map(m -> m.replace("\"", ""))
                    .toArray(String[]::new);

            if(!(tokens.length == 0 || tokens[0].length() == 0 || tokens[0].charAt(0) == '/' || tokens[0].charAt(0) == ';')){
                if(tokens[0].endsWith("_start")){
                    currentBlock = tokens[0].substring(0,tokens[0].length()-6);
                    parseBlockStart(tokens);
                }else if(tokens[0].endsWith("_end")){
                    parseBlockEnd(tokens);
                    currentBlock = "";
                }else{
                    currentAttribute = tokens[0];
                    parseBlockAttribute(tokens);
                }
            }
        });
    }

    protected abstract void parseBlockStart(String[] tokens);
    protected abstract void parseBlockEnd(String[] tokens);
    protected abstract void parseBlockAttribute(String[] tokens);
}
