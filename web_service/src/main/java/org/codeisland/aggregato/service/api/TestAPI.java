package org.codeisland.aggregato.service.api;

import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.response.NotFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lukas Knuth
 * @version 1.0
 */

@Api(
        name = "testapi",
        version = "v1",
        namespace = @ApiNamespace(
                ownerDomain = "aggregato.codeisland.org",
                ownerName = "aggregato.codeisland.org",
                packagePath = ""
        ),
        scopes = {Constants.EMAIL_SCOPE}, // Makes use of OAuth
        clientIds = { Constants.ANDROID_CLIENT_ID, Constants.IOS_CLIENT_ID, Constants.WEB_CLIENT_ID, Constants.API_EXPLORER_CLIENT_ID },
        audiences = { Constants.ANDROID_AUDIENCE }
)
public class TestAPI {

    public static List<Bean> beans = new ArrayList<>();

    static {
        beans.add(new Bean("Testing this bean"));
        beans.add(new Bean("The monkey is dead!"));
    }

    public Bean getBean(@Named("id") int id) throws NotFoundException {
        try {
            return beans.get(id);
        } catch (IndexOutOfBoundsException e) {
            throw new NotFoundException("bean with ID '"+id+"' not found!");
        }
    }

    public List<Bean> listBeans(){
        return beans;
    }

    @ApiMethod(name = "bean.multiply", httpMethod = "post")
    public Bean insertBean(@Named("times") int times, Bean bean){
        Bean resp = new Bean("");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times; i++){
            builder.append(bean.getData());
        }
        resp.setData(builder.toString());
        return resp;
    }

    @ApiMethod(name = "bean.authed", path = "bean/authed")
    public Bean authedBean(User user) throws Exception {
        // if "user" is null, user not authenticated!
        if (user == null){
            throw new Exception("User not authenticated!");
        } else {
            return new Bean("Hi, "+user.getEmail());
        }
    }

    @ApiMethod(name = "sayHi")
    public Bean sayHi(@Named("name") String name){
        return new Bean("Hi, "+name);
    }
}
