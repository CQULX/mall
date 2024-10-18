package com.lx.gulimall.member.dao;

import com.lx.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author luoxuan
 * @email 2644067844@qq.com
 * @date 2023-11-30 21:54:29
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
