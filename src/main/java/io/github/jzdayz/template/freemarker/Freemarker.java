package io.github.jzdayz.template.freemarker;


import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.*;
import java.util.Map;

public class Freemarker {

    private static final Configuration CONFIGURATION = new Configuration(Configuration.VERSION_2_3_22);

    static {
        try {
            CONFIGURATION.setDirectoryForTemplateLoading(new File(Freemarker.class.getResource("/template").toURI()));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        CONFIGURATION.setDefaultEncoding("UTF-8");
        CONFIGURATION.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }

    public static byte[] process(Map template, String relativePath) throws IOException, TemplateException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Writer out = new OutputStreamWriter(byteArrayOutputStream);
        Template temp = CONFIGURATION.getTemplate(relativePath);
        temp.process(template, out);
        return byteArrayOutputStream.toByteArray();
    }

}
