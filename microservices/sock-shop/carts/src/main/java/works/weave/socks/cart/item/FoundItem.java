/*
 * MIT License
 *
 * Copyright (c) 2020 manager
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

package works.weave.socks.cart.item;

import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import works.weave.socks.cart.entities.Item;

public class FoundItem implements Supplier<Item> {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final Supplier<List<Item>> items;
  private final Supplier<Item> item;

  public FoundItem(Supplier<List<Item>> items, Supplier<Item> item) {
    this.items = items;
    this.item = item;
  }

  @Override
  public Item get() {
    return items.get().stream()
        .filter(item.get()::equals)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Cannot find item in cart"));
  }

  public boolean hasItem() {
    boolean present = items.get().stream().anyMatch(item.get()::equals);
    log.debug((present ? "Found" : "Didn't find") + " item: " + item.get() + ", in: " + items.get());
    return present;
  }

}
