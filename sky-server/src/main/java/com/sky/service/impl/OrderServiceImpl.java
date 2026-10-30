package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.BaseException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Transactional
@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;// 订单Mapper
    @Autowired
    private OrderDetailMapper orderDetailMapper;// 订单明细Mapper
    @Autowired
    private AddressBookMapper addressBookMapper;// 地址簿Mapper
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;// 购物车Mapper
    @Autowired
    private UserMapper userMapper;// 用户Mapper
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;
    @Value("${sky.shop.address}")
    private String shopAddress;

    @Value("${sky.baidu.ak}")
    private String ak;



@Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //处理异常(购物车空的，地址为空)
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());// 根据id获取地址信息
        if (addressBook == null) {
            //地址为空
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //检查地址是否超出配送范围
        // TODO 本地未申请百度地图AK，暂时跳过配送范围校验
        // checkOutOfRange(addressBook.getCityName()
        //         + addressBook.getDistrictName()
        //         + addressBook.getDetail());
        //查询用户的购物车
    ShoppingCart shoppingCart =new ShoppingCart();
    Long userId = BaseContext.getCurrentId();// 获取当前用户id
    shoppingCart.setUserId(userId);// 设置用户id
    List<ShoppingCart> shoppingCartslist = shoppingCartMapper.list(shoppingCart);// 查询用户的购物车
    if (shoppingCartslist == null || shoppingCartslist.size() == 0) {
        //购物车为空
        throw new BaseException(MessageConstant.SHOPPING_CART_IS_NULL);
    }

    //向订单中插入一条数据
    Orders orders = new Orders();// 创建订单对象
    BeanUtils.copyProperties(ordersSubmitDTO, orders); // 将ordersSubmitDTO的属性复制到order中
    orders.setNumber(String.valueOf(System.currentTimeMillis())); // 生成订单号
    orders.setUserId(userId); // 设置用户id
    orders.setOrderTime(LocalDateTime.now());//设置订单时间
    orders.setPayStatus(Orders.UN_PAID);//设置支付状态为未支付
    orders.setStatus(Orders.PENDING_PAYMENT);//设置订单状态为待支付
    orders.setPhone(addressBook.getPhone()); // 设置电话
    orders.setConsignee(addressBook.getConsignee()); // 设置收货人

    orderMapper.insert(orders); // 向订单表中插入一条数据

        //向订单明细表中插入多条数据,由购物车决定
        List<OrderDetail> orderDetailList = new ArrayList<>(); // 创建订单明细列表
        for (ShoppingCart Cart : shoppingCartslist) {
            OrderDetail orderDetail = new OrderDetail();// 创建订单明细对象
            BeanUtils.copyProperties(Cart, orderDetail); // 将购物车数据复制到订单明细中
            orderDetail.setOrderId(orders.getId()); // 设置订单id
            orderDetailList.add(orderDetail);// 添加到订单明细列表中
        }
        orderDetailMapper.insertBatch(orderDetailList); // 向订单明细表中插入多条数据
        //清空购物车的数据
        shoppingCartMapper.deleteByUserId(userId);
        //封装VO返回结果

        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();



        return orderSubmitVO;
    }
    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id（user表主键）
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        // TODO 个人开发者无微信支付商户号，这里模拟支付成功
        Orders ordersDB = orderMapper.getByNumber(ordersPaymentDTO.getOrderNumber());
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (Orders.PAID.equals(ordersDB.getPayStatus())) {
            throw new OrderBusinessException("该订单已支付");
        }
        // 模拟微信回调：更新订单状态 + 来单提醒
        paySuccess(ordersPaymentDTO.getOrderNumber());

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", "ORDERPAID"); // 前端识别此标志可按"已支付"处理，不再调起 wx.requestPayment
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
        //向WebSocket发送消息 type orderId content
        Map map =new HashMap<>();
        map.put("type",1);//消息类型订单状态,1表示来单提醒，2表示客户催单
        map.put("orderId",orders.getId());//订单id
        map.put("content","订单号"+outTradeNo);//消息内容
        String json = JSON.toJSONString(map);//将map转换为json字符串
        webSocketServer.sendToAllClient(json);//发送消息，向页面发送json字符串

    }
    //分页查询
    public PageResult pageQueryUser(int pageNum, int pageSize, Integer status) {
        //分页查询
        PageHelper.startPage(pageNum, pageSize);//开启分页
        //封装查询条件
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();//创建订单查询条件对象
        ordersPageQueryDTO.setStatus(status); // 设置状态
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId()); // 设置用户id
        //查询订单
        Page<OrderVO> page = orderMapper.pageQueryUser(ordersPageQueryDTO);//把查到的订单放在page对象中
        //查询订单后知道了哪个订单，但是此时订单里面的信息还不知道，所以要查询这个订单的明细，
        //遍历page对象，查询每个订单的明细

        List<OrderVO> list = new ArrayList<>();//创建一个空的订单列表,目的是为了返回结果，用来接受查询到的订单
        if (page != null && page.size() > 0) {//如果page对象不为空且大小大于0
            for (Orders orders : page) // 遍历page对象，获取每个订单
            {
                //获取到订单id
                Long orderId = orders.getId();
                //查询订单明细
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);//查询订单明细

                OrderVO orderVO = new OrderVO();//创建一个空的订单VO对象
                BeanUtils.copyProperties(orders, orderVO);//将订单信息复制到orderVO对象中
                orderVO.setOrderDetailList(orderDetails);//将订单明细添加到orderVO对象中
                list.add(orderVO);//将orderVO对象添加到订单列表中
            }
        }

        //返回结果
        return new PageResult((int) page.getTotal(), list);//返回的是分页结果
    }
    //根据id查询订单
    public OrderVO details(Long id) {
        //查询订单
        Orders orders = orderMapper.getById(id);
        //查询该订单的明细
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orders.getId());
        // 将该订单及其详情封装到OrderVO并返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetails);

        return orderVO;

    }
    //用户取消订单
    public void userCancelById(Long id) throws Exception {
        //用户取消订单要遵循一下
        //1.首先判断这个id的订单是否存在
        //2.判断订单的状态，只能取消待接单、待取货状态的订单
        //3.如果订单是待接单状态，需要进行退款


        //根据id查询订单
        Orders orders = orderMapper.getById(id);
        //判断订单是否存在
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //判断订单状态
        if (orders.getStatus()>2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 订单处于待接单状态下取消，需要进行退款
        if (orders.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            // 调用微信支付接口，进行退款
            // TODO 测试环境未配置微信支付商户号，暂时跳过退款，避免空指针
            // weChatPayUtil.refund(
            //         orders.getNumber(), //商户订单号
            //         orders.getNumber(), //商户退款单号
            //         new BigDecimal(0.01),//退款金额，单位 元
            //         new BigDecimal(0.01));//原订单金额

        }
        //取消订单
        orders.setStatus(Orders.CANCELLED);
        //设置取消时间
        orders.setCancelTime(LocalDateTime.now());
        //设置取消原因
        orders.setCancelReason("用户取消");
        //更新订单
        orderMapper.update(orders);
    }
    /**
     * 再来一单
     *
     * @param id
     */
    public void repetition(Long id) {
        // 查询当前用户id
        Long userId = BaseContext.getCurrentId();
        //查询订单明细
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(id);
        //订单明细变成购物车
        List<ShoppingCart> shoppingCarts = orderDetails.stream().map((item) -> {
            ShoppingCart shoppingCart = new ShoppingCart();//创建一个空的购物车对象
            //因为id是购物车的主键，所以不能复制
            BeanUtils.copyProperties(item, shoppingCart,"id");//将订单明细复制到购物车中,忽略id
            shoppingCart.setUserId(userId);//设置用户id，目的是给购物车添加用户id
            return shoppingCart;//返回购物车对象
        }).collect(Collectors.toList());//将购物车列表添加到购物车中
        shoppingCartMapper.insertBatch(shoppingCarts);//向购物车表中插入多条数据
}
    //管理端查询订单
    /**
     * 订单搜索
     *
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);//分页查询订单列表

        // 部分订单状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> orderVOList = getOrderVOList(page);

        return new PageResult(page.getTotal(), orderVOList);
    }


    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        // 需要返回订单菜品信息，自定义OrderVO响应结果
        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getResult();//获取订单列表
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                // 将共同字段复制到OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);

                // 将订单菜品信息封装到orderVO中，并添加到orderVOList
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 根据订单id获取菜品信息字符串
     *
     * @param orders
     * @return
     */

    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }
    //统计订单数据
    @Override
    /**
     * 各个状态的订单数量统计
     *
     * @return
     */
    public OrderStatisticsVO statistics() {
        // 根据状态，分别查询出待接单、待派送、派送中的订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        // 将查询出的数据封装到orderStatisticsVO中响应
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }
     @Override
     /**
      * 接单
      *
      * @param ordersConfirmDTO
      */
     public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = new Orders();
        orders.setId(ordersConfirmDTO.getId());//获取订单id
        orders.setStatus(Orders.CONFIRMED);//设置订单状态为已接单
        orderMapper.update(orders);//更新订单状态，这个是修改数据库中的订单状态
     }
     @Override
     public void rejection(OrdersConfirmDTO ordersConfirmDTO) {
         // 根据id查询订单
         Orders ordersDB = orderMapper.getById(ordersConfirmDTO.getId());
         if (ordersDB == null) {
             throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
         }


        Orders orders = new Orders();
        orders.setId(ordersConfirmDTO.getId());//获取订单id
        orders.setCancelTime(LocalDateTime.now());//设置取消时间
        orders.setStatus(Orders.CANCELLED);//设置订单状态为已拒绝
        orderMapper.update(orders);//更新订单状态，这个是修改数据库中的订单状态
     }
     //取消订单
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
         //查询订单
         Orders orders = orderMapper.getById(ordersCancelDTO.getId());
         if (orders == null) {
             throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
         }
         orders.setId(ordersCancelDTO.getId());//获取订单id，目的是给订单设置id，方便后续更新订单状态
         orders.setStatus(Orders.CANCELLED);//设置订单状态为已取消
         orders.setCancelTime(LocalDateTime.now());//设置取消时间
         orders.setCancelReason(ordersCancelDTO.getCancelReason());//设置取消原因
         orderMapper.update(orders);
    }
    /**
     * 派送订单
     *
     * @param id
     */
    public void delivery(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为3
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orders);
    }
    /**
     * 完成订单
     *
     * @param id
     */
    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为4
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }


    /**
     * 检查客户的收货地址是否超出配送范围
     * @param address
     */

    private void checkOutOfRange(String address) {
        Map map = new HashMap();
        map.put("address",shopAddress);
        map.put("output","json");
        map.put("ak",ak);

        //获取店铺的经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("店铺地址解析失败");
        }

        //数据解析
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        //店铺经纬度坐标
        String shopLngLat = lat + "," + lng;

        map.put("address",address);
        //获取用户收货地址的经纬度坐标
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        jsonObject = JSON.parseObject(userCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("收货地址解析失败");
        }

        //数据解析
        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        //用户收货地址经纬度坐标
        String userLngLat = lat + "," + lng;

        map.put("origin",shopLngLat);
        map.put("destination",userLngLat);
        map.put("steps_info","0");

        //路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        jsonObject = JSON.parseObject(json);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("配送路线规划失败");
        }

        //数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if(distance > 5000){
            //配送距离超过5000米
            throw new OrderBusinessException("超出配送范围");
        }
    }

    /**
     * 客户催单
     *
     * @param id
     */
    @Override
    public void reminder(Long id) {
      // 根据id查询订单
      Orders orders = orderMapper.getById(id);
      if (orders == null) {
          throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
      }

      //
      Map map = new HashMap();// 创建一个Map对象，用于存储消息数据
      map.put("type", 2);// 消息类型，2表示催单
      map.put("orderId", id);// 订单id
      map.put("content", "订单号：" + orders.getNumber());// 催单内容
      webSocketServer.sendToAllClient(JSON.toJSONString(map));// 发送消息

    }

}
