/*
 * MIT License
 *
 * Copyright (c) 2020 usmanager
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package works.weave.socks.orders.controllers;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import works.weave.socks.orders.config.OrdersConfigurationProperties;
import works.weave.socks.orders.entities.Address;
import works.weave.socks.orders.entities.Card;
import works.weave.socks.orders.entities.Customer;
import works.weave.socks.orders.entities.CustomerOrder;
import works.weave.socks.orders.entities.Item;
import works.weave.socks.orders.entities.Shipment;
import works.weave.socks.orders.repositories.CustomerOrderRepository;
import works.weave.socks.orders.resources.NewOrderResource;
import works.weave.socks.orders.services.AsyncGetService;
import works.weave.socks.orders.values.PaymentRequest;
import works.weave.socks.orders.values.PaymentResponse;

@RepositoryRestController
public class OrdersController {

  private final Logger log = LoggerFactory.getLogger(getClass());

  @Autowired
  private OrdersConfigurationProperties config;

  @Autowired
  private AsyncGetService asyncGetService;

  @Autowired
  private CustomerOrderRepository customerOrderRepository;

  @Value(value = "${http.timeout:5}")
  private long timeout;

  @ResponseStatus(HttpStatus.CREATED)
  @RequestMapping(path = "/orders", consumes = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
  public @ResponseBody CustomerOrder newOrder(@RequestBody NewOrderResource item) {
    try {
      if (item.getAddress() == null
          || item.getCustomer() == null
          || item.getCard() == null
          || item.getItems() == null) {
        throw new InvalidOrderException("Invalid order request. Order requires customer, address, card and items.");
      }
      log.debug("Starting calls");
      Future<Address> addressFuture = asyncGetService.getJsonResource(item.getAddress(), new ParameterizedTypeReference
          <Address>() {
      });
      Future<Customer> customerFuture = asyncGetService.getJsonResource(item.getCustomer(), new ParameterizedTypeReference
          <Customer>() {
      });
      Future<Card> cardFuture = asyncGetService.getJsonResource(item.getCard(), new ParameterizedTypeReference
          <Card>() {
      });
      Future<List<Item>> itemsFuture = asyncGetService.getDataList(item.getItems(), new
          ParameterizedTypeReference<List<Item>>() {
          });
      log.debug("End of calls.");

      float amount = calculateTotal(itemsFuture.get(timeout, TimeUnit.SECONDS));

      // Call payment service to make sure they've paid
      PaymentRequest paymentRequest = new PaymentRequest(
          addressFuture.get(timeout, TimeUnit.SECONDS),
          cardFuture.get(timeout, TimeUnit.SECONDS),
          customerFuture.get(timeout, TimeUnit.SECONDS),
          amount);
      log.info("Sending payment request: " + paymentRequest);
      Future<PaymentResponse> paymentFuture = asyncGetService.postResource(
          config.getPaymentUri(),
          paymentRequest,
          new ParameterizedTypeReference<PaymentResponse>() {
          });
      PaymentResponse paymentResponse = paymentFuture.get(timeout, TimeUnit.SECONDS);
      log.info("Payment URI: " + config.getPaymentUri());
      log.info("Received payment response: " + paymentResponse);
      if (paymentResponse == null) {
        throw new PaymentDeclinedException("Unable to parse authorisation packet");
      }
      if (!paymentResponse.isAuthorised()) {
        throw new PaymentDeclinedException(paymentResponse.getMessage());
      }

      // Ship
      String customerId = customerFuture.get(timeout, TimeUnit.SECONDS).getId();
      Future<Shipment> shipmentFuture = asyncGetService.postResource(config.getShippingUri(), new Shipment(customerId),
          new ParameterizedTypeReference<Shipment>() { }
      );
      log.info("Shipping URI: " + config.getShippingUri());
      CustomerOrder order = new CustomerOrder(
          null,
          customerId,
          customerFuture.get(timeout, TimeUnit.SECONDS),
          addressFuture.get(timeout, TimeUnit.SECONDS),
          cardFuture.get(timeout, TimeUnit.SECONDS),
          itemsFuture.get(timeout, TimeUnit.SECONDS),
          shipmentFuture.get(timeout, TimeUnit.SECONDS),
          Calendar.getInstance().getTime(),
          amount);
      log.debug("Received data: " + order.toString());

      CustomerOrder savedOrder = customerOrderRepository.save(order);
      log.debug("Saved order: " + savedOrder);

      return savedOrder;
    } catch (TimeoutException e) {
      throw new IllegalStateException("Unable to create order due to timeout from one of the services.", e);
    } catch (InterruptedException | ExecutionException e) {
      throw new IllegalStateException("Unable to create order due to unspecified IO error.", e);
    }
  }

  private String parseId(String href) {
    Pattern idPattern = Pattern.compile("[\\w-]+$");
    Matcher matcher = idPattern.matcher(href);
    if (!matcher.find()) {
      throw new IllegalStateException("Could not parse user ID from: " + href);
    }
    return matcher.group(0);
  }

  /*TODO: Add link to shipping
  @RequestMapping(method = RequestMethod.GET, value = "/orders")
  public @ResponseBody
  ResponseEntity<?> getOrders() {
    List<CustomerOrder> customerOrders = customerOrderRepository.findAll();

    Resources<CustomerOrder> resources = new Resources<>(customerOrders);

    resources.forEach(r -> r);

    resources.add(linkTo(methodOn(ShippingController.class, CustomerOrder.getShipment::ge)).withSelfRel());

    // add other links as needed

    return ResponseEntity.ok(resources);
  }*/

  private float calculateTotal(List<Item> items) {
    float amount = 0F;
    float shipping = 4.99F;
    amount += items.stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice()).sum();
    amount += shipping;
    return amount;
  }

  @ResponseStatus(value = HttpStatus.NOT_ACCEPTABLE)
  public static class PaymentDeclinedException extends IllegalStateException {

    private static final long serialVersionUID = 7631228319212805183L;

    public PaymentDeclinedException(String s) {
      super(s);
    }
  }

  @ResponseStatus(value = HttpStatus.NOT_ACCEPTABLE)
  public static class InvalidOrderException extends IllegalStateException {

    private static final long serialVersionUID = -4138734462366684828L;

    public InvalidOrderException(String s) {
      super(s);
    }
  }

}