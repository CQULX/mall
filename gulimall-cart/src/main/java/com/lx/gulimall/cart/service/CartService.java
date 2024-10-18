package com.lx.gulimall.cart.service;

import com.lx.gulimall.cart.vo.Cart;
import com.lx.gulimall.cart.vo.CartItem;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface CartService {
    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    CartItem getCartItem(Long skuId);

    Cart getCart() throws ExecutionException, InterruptedException;


    void clearCart(String cartKey);

    void checkItem(Long skuId, int check);

    void changeItemCart(Long skuId, Integer num);

    void deleteItem(Long skuId);

    List<CartItem> getUserCartItems();
}
