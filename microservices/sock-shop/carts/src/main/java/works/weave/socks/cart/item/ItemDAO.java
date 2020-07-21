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

import java.util.HashMap;
import java.util.Map;

import works.weave.socks.cart.entities.Item;

public interface ItemDAO {

  Item save(Item item);

  void destroy(Item item);

  Item findOne(String id);

  class Fake implements ItemDAO {
    private final Map<String, Item> store = new HashMap<>();

    @Override
    public Item save(Item item) {
      return store.put(item.itemId(), item);
    }

    @Override
    public void destroy(Item item) {
      store.remove(item.itemId());

    }

    @Override
    public Item findOne(String id) {
      return store.entrySet().stream().filter(i -> i.getValue().id().equals(id)).map(Map.Entry::getValue)
          .findFirst().orElse(null);
    }
  }

}
