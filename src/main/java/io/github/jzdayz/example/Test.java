package io.github.jzdayz.example;

import io.github.jzdayz.template.freemarker.Freemarker;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Test {
    public static void main(String[] args) throws Exception{
        Map<String,Object> map = new HashMap<>();
        map.put("xixi", Arrays.asList(
                A.builder().age("12").name("xiaobai").build(),
                A.builder().age("18").name("hha").build()
                ));
        byte[] process = Freemarker.process(map, "index.html");
        System.out.println(new String(process));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class A{
        private String name;
        private String age;
    }
}
