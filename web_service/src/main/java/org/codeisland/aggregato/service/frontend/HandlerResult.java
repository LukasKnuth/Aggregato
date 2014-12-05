package org.codeisland.aggregato.service.frontend;

/**
 * The result returned by a {@link org.codeisland.aggregato.service.frontend.FrontendHandler}. This
 *  class encapsulates all information needed to render a result to the users Browser.
 * @author Lukas Knuth
 * @version 1.0
 */
public class HandlerResult {

    public static HandlerResult createFromTemplate(String page_title, String template_file, Object data){
        return new HandlerResult(page_title, null, data, template_file);
    }

    public static HandlerResult createFromString(String page_title, String content){
        return new HandlerResult(page_title, content, null, null);
    }

    public static HandlerResult createFromFormat(String page_title, String format, String... args){
        return createFromString(page_title, String.format(format, args));
    }

    private final String content;
    private final String page_title;
    private final Object data;
    private final String template_file;

    private HandlerResult(String page_title, String content, Object data, String template_file) {
        this.content = content;
        this.page_title = page_title;
        this.data = data;
        this.template_file = template_file;
    }

    boolean hasTemplate(){
        return this.template_file != null;
    }

    String getContent() {
        return content;
    }

    String getPageTitle() {
        return page_title;
    }

    Object getData() {
        return data;
    }

    String getTemplateFile() {
        return template_file;
    }
}
