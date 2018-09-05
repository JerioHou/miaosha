package com.jerio.miaosha.dao;

import com.jerio.miaosha.domain.MiaoshaOrder;
import com.jerio.miaosha.domain.OrderInfo;
import org.apache.ibatis.annotations.*;

/**
 * Created by Jerio on 2018/3/20
 */
@Mapper
public interface OrderDao {
    @Select("select * from miaosha_order where user_id=#{userId} and goods_id=#{goodsId}")
    public MiaoshaOrder getMiaoshaOrderByUserIdGoodsId(@Param("userId")long userId, @Param("goodsId")long goodsId);

    @Insert("insert into order_info(id,user_id, goods_id, goods_name, goods_count, goods_price, order_channel, status, create_date)values("
            + "#{id},#{userId}, #{goodsId}, #{goodsName}, #{goodsCount}, #{goodsPrice}, #{orderChannel},#{status},#{createDate} )")
    public long insert(OrderInfo orderInfo);

    @Insert("insert into miaosha_order (id,user_id, goods_id, order_id)values(#{id},#{userId}, #{goodsId}, #{orderId})")
    public int insertMiaoshaOrder(MiaoshaOrder miaoshaOrder);


    @Select("select * from order_info where id = #{orderId}")
    public OrderInfo getOrderById(@Param("orderId")long orderId);
}
