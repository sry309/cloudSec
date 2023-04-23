package com.common.aliyun.product;

import com.aliyun.rds20140815.Client;
import com.aliyun.rds20140815.models.*;
import com.domain.Key;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RDS {
    public static String privateType = "Private";
    public static String publicType = "Public";

    public static List<DescribeDBInstancesResponseBody.DescribeDBInstancesResponseBodyItemsDBInstance> getRDSLists(Key key) throws Exception {
        com.aliyun.rds20140815.Client client = getRdsClient(key);
        com.aliyun.rds20140815.models.DescribeDBInstancesRequest request = new com.aliyun.rds20140815.models.DescribeDBInstancesRequest();
        com.aliyun.teautil.models.RuntimeOptions runtime = new com.aliyun.teautil.models.RuntimeOptions();
        Map<String, DescribeRegionsResponseBody.DescribeRegionsResponseBodyRegionsRDSRegion> rdsRegion = getRdsRegion(key);
        java.util.List<DescribeDBInstancesResponseBody.DescribeDBInstancesResponseBodyItemsDBInstance> dbInstances = new ArrayList<>();
        for (String s : rdsRegion.keySet()) {
            request.setRegionId(rdsRegion.get(s).regionId);
            DescribeDBInstancesResponse describeDBInstancesResponse = client.describeDBInstancesWithOptions(request, runtime);
            if (describeDBInstancesResponse.body.items.DBInstance.size() >= 1){
                dbInstances.addAll(describeDBInstancesResponse.body.items.DBInstance);
            }
        }

        for (DescribeDBInstancesResponseBody.DescribeDBInstancesResponseBodyItemsDBInstance dbInstance : dbInstances) {
            System.out.println(dbInstance.DBInstanceId);
            descInstanceNetType(key,dbInstance.DBInstanceId);
        }
        return dbInstances;

    }
    private static Client getRdsClient(Key key) throws Exception {
        com.aliyun.teaopenapi.models.Config config = null;
        if (key.getToken() != null){
            config = new com.aliyun.teaopenapi.models.Config()
                    .setAccessKeyId(key.getSecretid())
                    .setAccessKeySecret(key.getSecretkey()).setSecurityToken(key.getToken());
        } else {
            config = new com.aliyun.teaopenapi.models.Config()
                    .setAccessKeyId(key.getSecretid())
                    .setAccessKeySecret(key.getSecretkey());
        }
        config.endpoint = "rds.aliyuncs.com";
        return new Client(config);
    }

    /**
     * 获取rds 可用region,期间需要去重，因为阿里云返回数据包含重复区域id，以map返回，方便后期使用
     * @param key
     * @return
     * @throws Exception
     */
    private static Map<String,DescribeRegionsResponseBody.DescribeRegionsResponseBodyRegionsRDSRegion> getRdsRegion(Key key) throws Exception {
        Client rdsClient = getRdsClient(key);
        Map<String,DescribeRegionsResponseBody.DescribeRegionsResponseBodyRegionsRDSRegion> map = new HashMap<>();
        com.aliyun.rds20140815.models.DescribeRegionsRequest request = new com.aliyun.rds20140815.models.DescribeRegionsRequest();
        com.aliyun.teautil.models.RuntimeOptions runtime = new com.aliyun.teautil.models.RuntimeOptions();
        DescribeRegionsResponse describeRegionsResponse = rdsClient.describeRegionsWithOptions(request, runtime);
        for (DescribeRegionsResponseBody.DescribeRegionsResponseBodyRegionsRDSRegion regionsRDSRegion : describeRegionsResponse.body.regions.RDSRegion) {
            if (!map.containsKey(regionsRDSRegion.regionId)){
                map.put(regionsRDSRegion.regionId,regionsRDSRegion);
            }
        }
        return map;
    }

    /**
     * 打开外网访问
     * @param key
     * @param instanceID
     * @throws Exception
     */
    public static void openWan(Key key,String instanceID) throws Exception {
        com.aliyun.rds20140815.Client client = getRdsClient(key);
        com.aliyun.rds20140815.models.AllocateInstancePublicConnectionRequest request = new com.aliyun.rds20140815.models.AllocateInstancePublicConnectionRequest()
                .setDBInstanceId(instanceID)
                .setPort("3306")
                .setConnectionStringPrefix(instanceID);
        com.aliyun.teautil.models.RuntimeOptions runtime = new com.aliyun.teautil.models.RuntimeOptions()
                .setReadTimeout(50000)
                .setConnectTimeout(50000);
        client.allocateInstancePublicConnectionWithOptions(request, runtime);
    }

    /**
     * 关闭外网访问
     * @param key
     * @throws Exception
     */
    public static void closeWan(Key key,String instanceID) throws Exception {
        com.aliyun.rds20140815.Client client = getRdsClient(key);
        com.aliyun.rds20140815.models.ReleaseInstancePublicConnectionRequest request = new com.aliyun.rds20140815.models.ReleaseInstancePublicConnectionRequest();
        request.setDBInstanceId(instanceID);
        request.setCurrentConnectionString(instanceID);
        com.aliyun.teautil.models.RuntimeOptions runtime = new com.aliyun.teautil.models.RuntimeOptions().setReadTimeout(50000).setConnectTimeout(50000);
        client.releaseInstancePublicConnectionWithOptions(request, runtime);
    }

    /**
     * 查看实例网络链接详情
     * @param key
     * @param instanceID
     * @return
     * @throws Exception
     */
    public static DescribeDBInstanceNetInfoResponse descInstanceNetType(Key key, String instanceID) throws Exception {
        com.aliyun.rds20140815.Client client = getRdsClient(key);
        com.aliyun.teautil.models.RuntimeOptions runtime = new com.aliyun.teautil.models.RuntimeOptions();
        com.aliyun.rds20140815.models.DescribeDBInstanceNetInfoRequest request = new com.aliyun.rds20140815.models.DescribeDBInstanceNetInfoRequest();
        request.setDBInstanceId(instanceID);
        DescribeDBInstanceNetInfoResponse describeDBInstanceNetInfoResponse = client.describeDBInstanceNetInfoWithOptions(request, runtime);
        return describeDBInstanceNetInfoResponse;
    }

    public static void main(String[] args) {
        Key key = new Key();
        key.setSecretid("LTAI5tCDaSyN8Ti469ig4JmA");
        key.setSecretkey("fBaer7haylKgoXwiHW8dfNBPObqR0H");
        try {
            getRDSLists(key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}