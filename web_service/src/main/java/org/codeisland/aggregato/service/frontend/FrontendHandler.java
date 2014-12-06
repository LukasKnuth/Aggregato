package org.codeisland.aggregato.service.frontend;

import com.google.api.client.util.Charsets;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.common.io.Files;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import com.timgroup.jgravatar.Gravatar;
import com.timgroup.jgravatar.GravatarDefaultImage;
import com.timgroup.jgravatar.GravatarRating;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.Writer;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class FrontendHandler extends HttpServlet {

    private static final String TEMPLATE_KEY = "aggregato-template:";
    private static final String BASE_TEMPLATE = "templates/base.html";
    private static final MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
    private static final UserService userService = UserServiceFactory.getUserService();

    protected HandlerResult handleGet(HttpServletRequest req) throws ServletException, IOException{
        return null;
    }
    protected HandlerResult handlePost(HttpServletRequest req) throws ServletException, IOException{
        return null;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HandlerResult result = handleGet(req);
        handle(req, resp, result);
    }
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HandlerResult result = handlePost(req);
        handle(req, resp, result);
    }

    /**
     * Handles writing data to the user, including login information.
     */
    private void handle(final HttpServletRequest req, HttpServletResponse resp, final HandlerResult handlerResult) throws ServletException, IOException{
        if (handlerResult == null){
            // Not implemented...
            return;
        }

        final String page_content;
        if (handlerResult.hasTemplate()){
            Template template = getTemplate(handlerResult.getTemplateFile());
            page_content = template.execute(handlerResult.getData());
        } else {
            page_content = handlerResult.getContent();
        }

        final Mustache.Lambda navigation_lambda = new Mustache.Lambda() {
            @Override
            public void execute(Template.Fragment fragment, Writer writer) throws IOException {
                String link = fragment.execute();
                if (link.contains(req.getRequestURI())){
                    writer.write(String.format("<li class=\"active\">%s</li>", link));
                } else {
                    writer.write(String.format("<li>%s</li>", link));
                }
            }
        };

        final Object user_info;
        final String login_logout_link;
        if (req.getUserPrincipal() != null){
            // User is logged in:
            final User user = userService.getCurrentUser();
            final Gravatar gravatar = new Gravatar(
                    32, GravatarRating.GENERAL_AUDIENCES, GravatarDefaultImage.IDENTICON
            );
            login_logout_link = userService.createLogoutURL(req.getRequestURI());
            user_info = new Object(){
                String name = user.getNickname();
                String avatar = gravatar.getUrl(user.getEmail());
            };
        } else {
            // Not logged in:
            login_logout_link = userService.createLoginURL(req.getRequestURI());
            user_info = null;
        }

        Object context = new Object(){
            String content = page_content;
            String title = handlerResult.getPageTitle();
            String loginLogoutLink = login_logout_link;
            Object user = user_info;
            Mustache.Lambda navitem = navigation_lambda;
        };

        resp.setCharacterEncoding("UTF-8");
        Template base_template = getTemplate(BASE_TEMPLATE);
        base_template.execute(context, resp.getWriter());
    }

    protected UserService getUserService(){
        return userService;
    }

    protected MemcacheService getMemcache(){
        return memcache;
    }

    // ------ TEMPLATE ENGINE ----

    private static Template getTemplate(String template_name) throws IOException {
        // TODO Replace nullValue with something user-friendly...
        return Mustache.compiler().nullValue("[null]").compile(getTemplateFileContents(template_name));
    }

    private static String getTemplateFileContents(String template_file) throws IOException {
        // TODO File I/O is discouraged and pretty slow...
        return Files.toString(new File(template_file), Charsets.UTF_8);/*
        if (memcache.contains(TEMPLATE_KEY+template_file)){
            return (String) memcache.get(TEMPLATE_KEY+template_file);
        } else {
            String template = Files.toString(new File(template_file), Charsets.UTF_8);
            memcache.put(TEMPLATE_KEY+template_file, template);
            return template;
        }*/
    }
}
