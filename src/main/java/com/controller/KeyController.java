package com.controller;


import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.common.Tools;
import com.common.Type;
import com.domain.Bucket;
import com.domain.Instance;
import com.domain.Key;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.service.BucketService;
import com.service.InstanceService;
import com.service.impl.BucketServiceImpl;
import com.service.impl.ConsoleUserServiceImpl;
import com.service.impl.DatabasesInstanceServiceImpl;
import com.service.impl.KeyServiceImpl;
import com.service.impl.tencent.PermissionService;
import com.service.impl.tencent.TencentInstanceService;
import com.tencentcloudapi.cam.v20190116.models.AttachPolicyInfo;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@SaCheckLogin
@RequestMapping("/api/ak")
public class KeyController {

    @Resource
    private KeyServiceImpl keyService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private InstanceService instanceService;
    @Resource
    private DatabasesInstanceServiceImpl databasesInstanceService;
    @Resource
    private BucketServiceImpl bucketService;
    @Resource
    private ConsoleUserServiceImpl consoleUserService;

    @RequestMapping(value = "/lists",method = {RequestMethod.GET,RequestMethod.OPTIONS})
    public SaResult getSecretList(@RequestParam(value = "page",defaultValue = "1",required = false) Integer page,
                                  @RequestParam(value = "limit",defaultValue = "10",required = false) Integer limit){
        QueryWrapper<Key> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("create_by_id",StpUtil.getLoginId());
        Page<Object> objects = PageHelper.startPage(page, limit);
        List<Key> list = keyService.list(queryWrapper);
        return SaResult.ok("获取成功").set("lists",list).set("total",objects.getTotal());
    }

    //获取厂商类型
    @RequestMapping(value = "/secret/type",method = {RequestMethod.GET,RequestMethod.OPTIONS})
    public SaResult getSecretType(){
        Type[] values = Type.values();
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("list",values);
        return SaResult.ok("获取成功").setData(hashMap);
    }
    @RequestMapping(value = "/add",method = {RequestMethod.POST})
    public SaResult addSecret(@RequestBody Key key){
        key.setCreateById(Integer.parseInt(StpUtil.getLoginId().toString()));
        if (keyService.saveKey(key))return SaResult.ok("添加完成");
        return SaResult.error("添加失败，名称重复");
    }

    @DeleteMapping("/del")
    public SaResult delSecret(@RequestParam("ids") String ids) throws UnsupportedEncodingException {
        String decode = URLDecoder.decode(ids,"UTF-8");
        List<Integer> collect = Arrays.stream(decode.split(","))
                .map(s -> Integer.parseInt(s.trim())).collect(Collectors.toList());
        for (Integer integer : collect) {
            keyService.removeById(integer);
            databasesInstanceService.delInstanceByKeyId(integer);
            bucketService.removeByKeyId(integer);
            consoleUserService.removeByKeyId(integer);
        }
        return SaResult.ok("删除成功");
    }
    @GetMapping("/update")
    public SaResult updateSecret(@RequestParam("id") Integer id){
        Key key = keyService.getById(id);
        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("row",key);
        return SaResult.ok().setData(hashMap);
    }
    @PostMapping("/update")
    public SaResult updateSecret(Key key){
        boolean aBoolean = keyService.updateById(key);
        if (aBoolean) return SaResult.ok("更新成功");
        return SaResult.error("更新失败");
    }

    /*
    获取权限列表
     */
    @RequestMapping("/perm")
    public SaResult getPermList(@RequestBody Map<String,String> id) throws Exception {
        List<Map> id1 = permissionService.getUserPermList(Integer.parseInt(id.get("id")), Integer.parseInt(StpUtil.getLoginId().toString()));
        return SaResult.ok().set("lists",id1);
    }
    @RequestMapping("/restart")
    public SaResult restartAkSk(@RequestBody Map<String,String> args){
        //插入之前删除之前存在的实例及cos信息
        String id = args.get("id");
        Key key = keyService.getById(Integer.parseInt(id));
        if (key.getCreateById().equals(Integer.parseInt(StpUtil.getLoginId().toString()))){
            cleanInstanceInfo(key);
            key.setCreateById(Integer.parseInt(StpUtil.getLoginId().toString()));
            Tools.executorService.submit(() -> keyService.execute(key));
            return SaResult.ok("更新成功，稍后刷新");
        }
        return SaResult.error("更新失败");
    }

    @GetMapping("/start")
    public SaResult startScan(){
        Object loginId = StpUtil.getLoginId();
        QueryWrapper<Key> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("create_by_id",loginId);
        List<Key> list = keyService.list(queryWrapper);
        for (Key key : list) {
            cleanInstanceInfo(key);
            key.setTaskStatus("等待检测");
            Tools.executorService.submit(() -> keyService.execute(key));
        }
        return SaResult.ok("启动成功");
    }

    @GetMapping("/stop")
    public SaResult stopTask(){
        Tools.stopALLTask();
        List<Key> list = keyService.list();
        for (Key key : list) {
            key.setTaskStatus("停止检测");
            keyService.updateById(key);
            cleanInstanceInfo(key);
        }
        return SaResult.ok("停止成功");
    }

    private void cleanInstanceInfo(Key key) {
        QueryWrapper<Instance> instanceQueryWrapper = new QueryWrapper<>();
        instanceQueryWrapper.eq("key_id", key.getId());
        instanceService.remove(instanceQueryWrapper);
        QueryWrapper<Bucket> bucketQueryWrapper = new QueryWrapper<>();
        bucketQueryWrapper.eq("key_id", key.getId());
        bucketService.remove(bucketQueryWrapper);
        databasesInstanceService.delInstanceByKeyId(key.getId());
    }

}
