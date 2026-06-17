package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItem;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.payload.CartDTO;
import com.ecommerce.project.payload.CartItemDTO;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.repositories.CartItemRepository;
import com.ecommerce.project.repositories.CartRepository;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.util.AuthUtil;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    ModelMapper mapper;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Override
    public CartDTO addProductToCart(Long productId, Integer quantity) {
        Cart cart = createCart();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product","productId",productId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId);

        if(cartItem != null) {
            throw new APIException("Product "+ product.getProductName()+" already exists");
        }

        if(product.getQuantity() == 0) {
            throw new APIException(product.getProductName()+" is not available");
        }

        if(product.getQuantity() < quantity) {
            throw new APIException("Please, make an order of the "+product.getProductName()+" less than or equal to "+quantity+".");
        }

        CartItem newCartItem = new CartItem();
        newCartItem.setProduct(product);
        newCartItem.setQuantity(quantity);
        newCartItem.setCart(cart);
        newCartItem.setDiscount(product.getDiscount());
        newCartItem.setProductPrice(product.getSpecialPrice());
        cartItemRepository.save(newCartItem);

        cart.setTotalPrice(cart.getTotalPrice() + product.getSpecialPrice() * quantity);

        cartRepository.save(cart);

        CartDTO cartDTO = mapper.map(cart,CartDTO.class);
        List<CartItem> cartItems= cart.getCartItems();
        Stream<ProductDTO> productDTOStream = cartItems.stream().map(item -> {
            ProductDTO productDTO = mapper.map(item,ProductDTO.class);
            productDTO.setQuantity(item.getQuantity());
            return productDTO;
        });

        cartDTO.setProducts(productDTOStream.toList());
        return cartDTO;
    }

    @Override
    public List<CartDTO> getAllCarts() {
        List<Cart> carts = cartRepository.findAll();
        if(carts.isEmpty()) {
            throw new APIException("No carts found");
        }

        List<CartDTO> cartDTOS = carts.stream()
                .map(cart -> {
                    CartDTO cartDTO = mapper.map(cart,CartDTO.class);
                    List<ProductDTO> products = cart.getCartItems().stream().map(cartItem -> {
                        ProductDTO productDTO = mapper.map(cartItem,ProductDTO.class);
                        productDTO.setQuantity(cartItem.getQuantity());
                        return productDTO;
                    }).toList();
                    cartDTO.setProducts(products);
                    return cartDTO;
                }).toList();

        return cartDTOS;
    }

    @Override
    public CartDTO getCart(String emailId, Long cartId) {
        Cart cart = cartRepository.findCartByEmailAndCartId(emailId, cartId);
        if(cart == null) {
            throw new ResourceNotFoundException("Cart","cartId",cartId);
        }

        CartDTO cartDTO = mapper.map(cart,CartDTO.class);
        cart.getCartItems().forEach(cartItem -> {cartItem.getProduct().setQuantity(cartItem.getQuantity());});
        List<ProductDTO> products = cart.getCartItems().stream()
                .map(p -> mapper.map(p.getProduct(), ProductDTO.class))
                .toList();

        cartDTO.setProducts(products);
        return cartDTO;
    }

    @Override
    @Transactional
    public CartDTO updateProductQuantityInCart(Long productId, Integer quantity) {
        String emailId = authUtil.loggedInUser().getEmail();
        Cart userCart = cartRepository.findCartByEmail(emailId);
        Long cartId = userCart.getCartId();
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart","cartId",cartId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product","productId",productId));

        if(product.getQuantity() == 0) {
            throw new APIException(product.getProductName()+" is not available");
        }

        if(product.getQuantity() < quantity) {
            throw new APIException("Please, make an order of the "+product.getProductName()+" less than or equal to "+quantity+".");
        }

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);
        if(cartItem == null) {
            throw new APIException("Product "+ product.getProductName()+" not avilable in the cart");
        }

        int newQuantity = cartItem.getQuantity() + quantity;

        if(newQuantity < 0){
            throw new APIException("The quantity cannot be less than 0");
        }

        if(newQuantity == 0){
            deleteProductFromCart(cartId, productId);
            cart = cartRepository.findById(cartId).get();
        } else{
            if (product.getQuantity() < newQuantity) {
                throw new APIException("Please, make an order of the " + product.getProductName() + " less than or equal to " + product.getQuantity() + ".");
            }
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setQuantity(cartItem.getQuantity() +  quantity);
            cartItem.setDiscount(product.getDiscount());

            cart.setTotalPrice(cart.getTotalPrice() + (cartItem.getProductPrice() * quantity));

            cartItemRepository.save(cartItem);
            cartRepository.save(cart);
        }

        CartItem updatedCartItem = cartItemRepository.save(cartItem);
        if(updatedCartItem.getQuantity() == 0){
            cartItemRepository.deleteById(updatedCartItem.getCartItemId());
        }

        CartDTO cartDTO = mapper.map(cart, CartDTO.class);
        List<ProductDTO> productDTOs = cart.getCartItems().stream().map(item -> {
            ProductDTO pDto = mapper.map(item.getProduct(), ProductDTO.class);
            pDto.setQuantity(item.getQuantity()); // Show quantity in cart, not stock
            return pDto;
        }).toList();

        cartDTO.setProducts(productDTOs);
        return cartDTO;
    }

    @Transactional
    @Override
    public String deleteProductFromCart(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart","cartId", cartId));
        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

        if(cartItem == null) {
            throw new ResourceNotFoundException("Product","productId",productId);
        }

        cart.setTotalPrice(cart.getTotalPrice() - (cartItem.getProductPrice() * cartItem.getQuantity()));
        cartItemRepository.deleteCartItemByProductIdAndCartId(cartId, productId);

        return "Product "+ cartItem.getProduct().getProductName() +" has been removed from the cart";
    }

    @Override
    public void updateProductInCarts(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart","cartId",cartId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product","productId",productId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);
        if(cartItem == null) {
            throw new APIException("Product "+ product.getProductName()+" is not available in the cart");
        }

        double cartPrice = cart.getTotalPrice() - (cartItem.getProductPrice() * cartItem.getQuantity());

        cartItem.setProductPrice(product.getSpecialPrice());

        cart.setTotalPrice(cartPrice + (cartItem.getProductPrice() * cartItem.getQuantity()));

        cartItem = cartItemRepository.save(cartItem);
    }

    @Transactional
    @Override
    public String createOrUpdateCartWithItems(List<CartItemDTO> cartItems) {
        String emailId = authUtil.loggedInEmail();
        Cart existingCart = cartRepository.findCartByEmail(emailId);
        if(existingCart == null) {
            existingCart = new Cart();
            existingCart.setTotalPrice(0.00);
            existingCart.setUser(authUtil.loggedInUser());
            existingCart = cartRepository.save(existingCart);
        }
        else {
            cartItemRepository.deleteAllByCartId(existingCart.getCartId());
        }
        double totalPrice = 0.0;

        for(CartItemDTO cartItemDTO : cartItems) {
            Long productId = cartItemDTO.getProductId();
            Integer quantity = cartItemDTO.getQuantity();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product","productId",productId));

            //product.setQuantity(product.getQuantity() - quantity);
            totalPrice +=  product.getSpecialPrice() * quantity;

            CartItem cartItem = new CartItem();
            cartItem.setProduct(product);
            cartItem.setCart(existingCart);
            cartItem.setQuantity(quantity);
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setDiscount(product.getDiscount());

            cartItemRepository.save(cartItem);
        }

        existingCart.setTotalPrice(totalPrice);
        cartRepository.save(existingCart);

        return "Cart has been updated/created";
    }

    private Cart createCart() {
        Cart userCart = cartRepository.findCartByEmail(authUtil.loggedInEmail());
        if (userCart != null) {
            return  userCart;
        }
        Cart cart = new Cart();
        cart.setTotalPrice(0.0);
        cart.setUser(authUtil.loggedInUser());

        return cartRepository.save(cart);
    }
}
