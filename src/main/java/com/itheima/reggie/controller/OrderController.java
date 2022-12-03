package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.OrdersDto;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.service.OrderDetailService;
import com.itheima.reggie.service.OrderService;
import com.itheima.reggie.service.UserService;

import lombok.extern.slf4j.Slf4j;

import net.sf.jsqlparser.expression.DateTimeLiteralExpression;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.jaxb.SpringDataJaxb;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单
 */
@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private UserService userService;
    @Autowired
    private OrderDetailService orderDetailService;

    /**
     * 用户下单
     * @param orders
     * @return
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders){
        log.info("订单数据：{}",orders);
        orderService.submit(orders);
        return R.success("下单成功");
    }
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize, String number,String beginTime,String endTime){
        //分页构造器对象
        Page<Orders> pageInfo = new Page<>(page,pageSize);
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        //添加查询条件，根据name进行like模糊查询
        queryWrapper.like(number != null,Orders::getNumber,number);
        queryWrapper.ge(beginTime!=null,Orders::getOrderTime,beginTime);
        queryWrapper.le(endTime!=null,Orders::getOrderTime,endTime);
        //添加排序条件，根据更新时间降序排列
        queryWrapper.orderByDesc(Orders::getCheckoutTime);

        orderService.page(pageInfo,queryWrapper);

        //对象拷贝
        List<Orders> records = pageInfo.getRecords();

        records=records.stream().map((item) ->{
            Long userId = item.getUserId();
            User user = userService.getById(userId);
            String name = user.getName();
            item.setUserName(name);
            return item;
        }).collect(Collectors.toList());

        return R.success(pageInfo);
    }
    @GetMapping("/userPage")
    public R<Page> userPage(int page,int pageSize){
        Page<Orders> pageInfo = new Page<>(page,pageSize);
        Page<OrdersDto> pageDto = new Page<>(page,pageSize);
        Long currentId = BaseContext.getCurrentId();
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getUserId,currentId);
        queryWrapper.orderByDesc(Orders::getOrderTime);
        orderService.page(pageInfo,queryWrapper);
        LambdaQueryWrapper<OrderDetail> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        List<Orders> records = pageInfo.getRecords();
        List<OrdersDto> ordersDtoList = records.stream().map((item) ->{
            OrdersDto ordersDto = new OrdersDto();
            Long id = item.getId();
            LambdaQueryWrapper<OrderDetail> queryWrapper1 = new LambdaQueryWrapper<>();
            queryWrapper1.eq(OrderDetail::getOrderId,id);
            List<OrderDetail> list = orderDetailService.list(queryWrapper1);
            BeanUtils.copyProperties(item,ordersDto);
            ordersDto.setOrderDetails(list);
            return ordersDto;
        }).collect(Collectors.toList());
        BeanUtils.copyProperties(pageInfo,pageDto,"records");
        pageDto.setRecords(ordersDtoList);
        return R.success(pageDto);
    }

}