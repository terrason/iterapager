# IteraPager 使用说明

`IteraPager` 是一个用于处理大规模数据分页的工具类，将分页生产者包装成 `Iterable` 对象。它可以帮助开发者高效地处理无限大的数据集合，避免一次性加载所有数据到内存中。

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## 关键点：

- 懒加载：按需分页加载数据，避免一次性加载全部数据。
- 流式处理：提供 `flat()` 和 `concat()` 方法，支持类似集合的遍历方式。
- 有序优化：`IteraPager.ofOrdered()` 方法允许基于上一批数据的末尾元素优化查询性能（如 SQL 中的 `WHERE id > lastId` 以适配索引）。
- 内存安全：严格限制每批数据量（batchSize），防止生产者返回过量数据。

## 使用说明
  在项目的 `pom.xml` 中添加以下依赖：
  ```xml
  <dependency>
    <groupId>io.github.terrason</groupId>
    <artifactId>iterapager</artifactId>
    <version>1.0.1</version>
  </dependency>
  ```

以下示例展示了如何将无限大小的数据集合包装到 `IteraPager` 对象中，并使用 `for ... in ...` 遍历数据逐一处理。

### 示例 1: 使用 `new IteraPager(int, Function)` 构造函数初始化

```java
import com.capsulode.iterapager.IteraPager;

import java.util.List;

public class Example1 {
    public static void main(String[] args) {
        //定义每页数据大小,这个参数会用于判断数据是否到达末尾，
        // 如果生产一批数据集合的大小少于这个值，就认为到达末尾了，
        // 后续不再调用生产者生产数据。千万不要大于这个值。
        int batchSize = 512;

        // 使用构造函数创建 IteraPager 对象, pageable 表示当前分页批次
        IteraPager<YourGracefulBean> pager = new IteraPager<>(batchSize, pageable -> {
            // 定义如何加载数据。 
            // pageable.page() - 当前页码，从1开始； 
            // pageable.limit() 分页大小，与batchSize一致；
            // pageable.offset() - 当前数据偏移量，从0开始；
            List<YourGracefulBean> onePageBeans = dataService.loadData(pageable.page(), pageable.limit());
            // 这批数据列表长度应等于batchSize，如果小于这个值，就认为到达末尾了，这个lambda函数不再执行；
            // 如果大于batchSize, 必然是错误的，抛出 UnsupportedOperationException 异常。
            // 还需要注意后续迭代中对数据库造成的修改不要影响这个分页查询结果。
            return onePageBeans;
        });

        // 使用 for-each 遍历数据并逐一处理
        for (YourGracefulBean obj : pager.flat()) {
            System.out.println(obj); // 演示打印
            //此处处理数据的业务代码，只要别缓存obj对象，obj可正常被垃圾回收
        }
    }
}
```

### 示例 2: 使用 IteraPager.ofOrdered(int, DataProducer) 静态方法初始化

在这个示例中，我们利用上一批数据的最后一个元素来优化查询性能。

```java
import com.capsulode.iterapager.Pager;

public class Example2 {
    public static void main(String[] args) {
    // 使用 `IteraPager.ofOrdered` 创建 IteraPager 对象,
    // p 表示分页参数（同示例1的pageable） 
    // lastData 表示上次查询的最后一个元素，第一次查询为 null
        Pager<YourGracefulBean> pager = Pager.ofOrdered(500,
                (p, lastData) -> archiveService.findProcessableEfiles(lastData, p.getLimit()));
        /* 这里将最后一次数据传给业务服务层，后续代码需判断lastData是否为null，
          再决定sql中是否要拼接where条件，例如mybatis可以这样：
        
          <select id="selectEfileIdByStates" resultType="com.capsulode.demo.model.YourGracefulBean">
              select id, state, order_num
              from efile
              <where>
                <if test="lastOrderNum != null">
                  and order_num &gt; #{lastOrderNum}
                </if>
                <!-- 其他业务条件 -->
              </where>
              order by order_num
              limit #{limit}
          </select>
        
          其中order_num为唯一约束字段，从而有效利用索引
        */

        // 使用 for-each 遍历数据并逐一处理
        for (YourGracefulBean obj : pager.flat()) {
            System.out.println(obj); // 演示打印
            //此处处理数据的业务代码，只要别缓存obj对象，obj可正常被垃圾回收
        }
    }
}
```

通过以上示例，您可以轻松地将大规模数据分页处理逻辑集成到您的项目中，需要关注要点：

- 需要提供生产者生产数据，每批数据不可大于`batchSize`，如果生产的数据量小于`batchSize`将认为到达数据末尾，后续不再调用生产者生产数据。
- 提供便捷的迭代器和流式操作，像普通Collection一样使用 `for ... in ...` 遍历，不必关心内部实现（实际上所有数据是懒加载的）。
- 生产者可利用上一批次的最后一个元素来优化查询性能，


## 注意事项

  - 生产者的实现

    生产者需要确保每次返回的数据量不超过批处理大小（batchSize），否则抛出`UnsupportedOperationException`异常。
    每次返回的数据量应该等于batchSize，如果小于它，则认为数据已到达末尾，后续不再调用生产者生产数据，
    `for...in...` 循环会在这批数据末尾停止。

  - 有序分页

    在使用 ofOrdered 方法时，生产者可以利用上一批数据的最后一个元素优化查询性能，
    例如在 SQL 查询中使用 `WHERE id > lastId` 。

  - 内存管理

    IteraPager 通过批量加载数据避免了内存溢出问题，但仍需注意:
      - 生产者的实现是否高效；
      - 生产者的查询语句是否正确遍历数据（重复和遗漏数据）；
      - 消费数据时是否内存溢出（如导出大量数据时未选用SXSSFWorkbook）