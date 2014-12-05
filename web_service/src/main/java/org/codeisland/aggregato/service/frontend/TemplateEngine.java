package org.codeisland.aggregato.service.frontend;

import com.google.api.client.util.Charsets;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.common.io.Files;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class TemplateEngine {

    private TemplateEngine(){}

    private static final String TEMPLATE_KEY = "aggregato-template:";
    private static final String BASE_TEMPLATE = "templates/base.html";
    private static final MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();

    public static void writeTemplate(Writer out, String page_title, String template_file, Object data) throws IOException {
        Template page_template = getTemplate(template_file);
        writeString(out, page_title, page_template.execute(data));
    }

    public static void writeFormat(Writer out, String page_title, String format_string, String... args) throws IOException {
        writeString(out, page_title, String.format(format_string, args));
    }

    public static void writeString(Writer out, final String page_title, final String page_content) throws IOException {
        Template base_template = getTemplate(BASE_TEMPLATE);
        base_template.execute(new Object(){
            String content = page_content;
            String title = page_title;
        }, out);
    }

    private static Template getTemplate(String template_name) throws IOException {
        return Mustache.compiler().compile(getTemplateFileContents(template_name));
    }

    private static String getTemplateFileContents(String template_file) throws IOException {
        if (memcache.contains(TEMPLATE_KEY+template_file)){
            return (String) memcache.get(TEMPLATE_KEY+template_file);
        } else {
            String template = Files.toString(new File(template_file), Charsets.UTF_8);
            memcache.put(TEMPLATE_KEY+template_file, template);
            return template;
        }
    }
}
