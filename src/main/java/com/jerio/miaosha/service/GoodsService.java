package com.jerio.miaosha.service;

import com.jerio.miaosha.dao.GoodsDao;
import com.jerio.miaosha.domain.MiaoshaGoods;
import com.jerio.miaosha.redis.GoodsKey;
import com.jerio.miaosha.redis.RedisService;
import com.jerio.miaosha.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by Jerio on 2018/3/20.
 */
@Service
public class GoodsService {

    @Autowired
    GoodsDao goodsDao;
    @Autowired
    RedisService redisService;

    public List<GoodsVo> listGoodsVo(){
        return goodsDao.listGoodsVo();
    }

    public GoodsVo getGoodsVoByGoodsId(long goodsId) {
        GoodsVo goodsVo = redisService.get(GoodsKey.GoodsVo,goodsId+"",GoodsVo.class);
        if (goodsVo == null){
            goodsVo = goodsDao.getGoodsVoByGoodsId(goodsId);
            redisService.set(GoodsKey.GoodsVo,goodsId+"",goodsVo);
        }
        return goodsVo;
    }

    public boolean reduceStock(GoodsVo goods) {
        MiaoshaGoods g = new MiaoshaGoods();
        g.setGoodsId(goods.getId());
        int ret = goodsDao.reduceStock(g);
        return ret > 0;
    }



}

