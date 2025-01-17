package com.controller;


import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.common.RandomPwd;
import com.domain.DatabasesInstance;
import com.domain.Key;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.service.KeyService;
import com.service.impl.DatabasesInstanceServiceImpl;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.*;

@RestController
@SaCheckLogin
@RequestMapping("/api/databases")
public class DatabasesController {
    @Resource
    private DatabasesInstanceServiceImpl databasesInstanceService;
    @Resource
    private KeyService keyService;
    @RequestMapping("/lists")
    public SaResult getMysqlLists(@RequestParam(required = false) String quick_search,
                                  @RequestParam(value = "page",defaultValue = "1",required = false) Integer page,
                                  @RequestParam(value = "limit",defaultValue = "10",required = false) Integer limit){
        List<DatabasesInstance> databasesList = new ArrayList<>();
        Page<DatabasesInstance> objects = PageHelper.startPage(page, limit);
        if (quick_search != null){
            QueryWrapper<Key> keyQueryWrapper = new QueryWrapper<>();
            keyQueryWrapper.eq("name",quick_search);
            Key one = keyService.getOne(keyQueryWrapper);
            if (one != null){
                QueryWrapper<DatabasesInstance> databasesInstanceQueryWrapper = new QueryWrapper<>();
                databasesInstanceQueryWrapper.eq("key_id",one.getId());
                databasesList = databasesInstanceService.list(databasesInstanceQueryWrapper);
            }
        }else{
            databasesList = databasesInstanceService.list();
        }
        return SaResult.ok().set("lists", databasesList).set("total",objects.getTotal());
    }
    @RequestMapping("/open")
    public SaResult openWanService(@RequestBody Map<String,Integer> args){
        Integer id = args.get("id");
        DatabasesInstance databasesById = databasesInstanceService.getById(id);
        QueryWrapper<Key> keyQueryWrapper = new QueryWrapper<>();
        keyQueryWrapper.eq("id",databasesById.getKeyId());
        Key one = keyService.getOne(keyQueryWrapper);
        String s = null;
        if (Integer.parseInt(StpUtil.getLoginId().toString()) == one.getCreateById()){
            s = databasesInstanceService.openWan(id);
        }
        if (s != null){
            return SaResult.error(s);
        }else {
            return SaResult.ok("开启成功,预计30S后刷新数据库..");
        }
    }
    @RequestMapping("/close")
    public SaResult closeWanService(@RequestBody Map<String,Integer> args){
        Integer id = args.get("id");
        String s = databasesInstanceService.closeWan(id);
        if (s!=null) return SaResult.error(s);
        return SaResult.ok("关闭成功,请重新执行检测权限操作");
    }

    @RequestMapping("/user")
    public SaResult createUser(@RequestBody Map<String,String> args){
        String id = args.get("id");
        DatabasesInstance mysqlById = databasesInstanceService.getInstanceById(Integer.valueOf(id));
        Random r = new Random();
        int i = r.nextInt(100);
        try {
            databasesInstanceService.createDBUser(Integer.parseInt(id), "test" + i, RandomPwd.getRandomPwd(8));
            return SaResult.ok("创建成功");
        } catch (Exception e) {
            return SaResult.error("创建失败，原因：" + e.getMessage());
        }

//        if (mysqlById.getType().equals(DatabasesInstanceServiceImpl.postgres)){
//
//            try {
//                String dbUser = databasesInstanceService.createDBUser(Integer.parseInt(id), username, RandomPwd.getRandomPwd(8));
//                if (dbUser != null){
//                    return SaResult.error(dbUser);
//                }
//                return SaResult.ok("创建成功");
//            } catch (Exception e) {
//                return SaResult.error("创建失败，原因：" + e.getMessage());
//            }
//        }
//        try {
//
//            String dbUser = databasesInstanceService.createDBUser(Integer.parseInt(id), "test" + i, RandomPwd.getRandomPwd(8));
//            if (dbUser != null){
//                return SaResult.error(dbUser);
//            }
//            return SaResult.ok("创建成功");
//        } catch (TencentCloudSDKException e) {
//            return SaResult.error("创建失败，原因：" + e.getMessage());
//        }
    }

    @RequestMapping("/userLists")
    public SaResult getUserLists(@RequestBody Map<String,String> args){
        String id = args.get("id");
        return SaResult.ok().set("lists",databasesInstanceService.getUserLists(Integer.valueOf(id)));
    }
}
