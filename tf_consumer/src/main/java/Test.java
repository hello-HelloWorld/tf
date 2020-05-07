

/*
 * @author: sunxiaoxiong
 * @date  : Created in 2020/4/30 15:53
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import java.util.Map;

public class Test {
    public static void main(String[] args) {
        String li = "{\"monitor_id\":\"0013\",\"speed\":\"043\"}";
        // Map map = JSON.parseObject(li, Map.class);
        Map<String, String> map = JSON.parseObject(li, new TypeReference<Map<String, String>>() {
        });
        for (Object obj : map.keySet()) {
            System.out.println(obj);
            System.out.println(map.get(obj));
        }

    }
}
