package org.fulin.chestnut;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Random;

import static org.fulin.ChestnutApplication.metricRegistry;
import static org.fulin.chestnut.Response.*;

/**
 * chestnut
 *
 * @author tangfulin
 * @since 17/3/3
 */
@RestController
@RequestMapping(path = "")
public class PccController {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    PccService pccService;

    String[] actions = new String[]{"like", "is_like", "count", "list"};
    Random random = new Random(System.currentTimeMillis());

    @RequestMapping(path = "/pcc")
    public Response action(@RequestParam(value = "action") String action,
                           @RequestParam(value = "uid", defaultValue = "0", required = false) long uid,
                           @RequestParam(value = "oid", defaultValue = "0", required = false) long oid,
                           @RequestParam(value = "page_size", defaultValue = "20", required = false) int pageSize,
                           @RequestParam(value = "is_friend", defaultValue = "0", required = false) int isFriend) {
        try {

            metricRegistry.counter("action." + action).inc();

            if (action.toLowerCase().startsWith("press")) {
                if (action.equalsIgnoreCase("press")) {
                    int p = Math.abs(random.nextInt()) % actions.length;
                    action = actions[p];
                } else {
                    action = action.substring("press_".length());
                }
                uid = Math.abs(random.nextInt()) % 10000000;
                oid = Math.abs(random.nextInt()) % 10000000;
                pageSize = Math.abs(random.nextInt()) % 20;
                isFriend = Math.abs(random.nextInt()) % 2;
            }

            if (action.equalsIgnoreCase("like")) {
                return like(uid, oid);
            }

            if (action.equalsIgnoreCase("is_like")) {
                return isLike(uid, oid);
            }

            if (action.equalsIgnoreCase("count")) {
                return count(oid);
            }

            if (action.equalsIgnoreCase("list")) {
                if (isFriend > 0) {
                    return listFriend(oid, pageSize, uid);
                } else {
                    return list(oid, pageSize);
                }
            }

            metricRegistry.counter("error.client").inc();

            return CLIENT_ERROR_RESPONSE;
        } catch (Exception e) {
            logger.error("error for action {}", action, e);

            metricRegistry.counter("error.server").inc();

            return SERVER_ERROR_RESPONSE;
        }
    }

    // return the oid 's liked uid list
    // return error for uid already like oid
    @Timed
    @RequestMapping(path = "/pcc/like")
    public Response<List<User>> like(long uid, long oid) {
        if (pccService.isLike(uid, oid)) {
            metricRegistry.counter("error.already_like").inc();

            return ALREADY_LIKE_ERROR_RESPONSE;
        }
        return Response.of("like", uid, oid, pccService.getUsers(pccService.like(uid, oid)));
    }

    // 1 for like, 0 for not
    @Timed
    @RequestMapping(path = "/pcc/is_like")
    public Response<Integer> isLike(long uid, long oid) {
        int result = pccService.isLike(uid, oid) ? 1 : 0;
        return Response.of("is_like", uid, oid, result);
    }

    @RequestMapping(path = "/pcc/count")
    @Timed
    public Response<Long> count(long oid) {
        return Response.of("count", 0, oid, pccService.count(oid));
    }

    @RequestMapping(path = "/pcc/list")
    @Timed
    public Response<List<User>> list(long oid, int pageSize) {
        return Response.of("list", 0, oid,
                pccService.getUsers(pccService.list(oid, pageSize)));
    }

    @RequestMapping(path = "/pcc/list_friend")
    @Timed
    public Response<List<User>> listFriend(long oid, int pageSize, long uid) {
        return Response.of("list_friend", uid, oid,
                pccService.getUsers(pccService.listFriend(oid, pageSize, uid)));
    }

    @Timed
    @RequestMapping(path = "/pcc/add_user")
    public Response<User> addUser(@RequestParam(value = "uid") long uid,
                                  @RequestParam(value = "nickname") String nickname) {
        return Response.of("add_user", uid, 0, pccService.addUser(uid, nickname));
    }

    @Timed
    @RequestMapping(path = "/pcc/add_friend")
    public Response<List<User>> addFriend(@RequestParam(value = "uid") long uid,
                                          @RequestParam(value = "friend_uid") long friendUid) {
        pccService.addFriend(uid, friendUid);
        List<User> friends = pccService.getUsers(pccService.getFriend(uid));

        return Response.of("add_user", uid, 0, friends);
    }

}
