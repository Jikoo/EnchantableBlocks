package com.github.jikoo.enchantableblocks.mock.answer;

import static org.mockito.Mockito.spy;

import org.jetbrains.annotations.NotNull;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class SpiedAnswer<T> implements Answer<T> {

  @Override
  public T answer(@NotNull InvocationOnMock invocation) throws Throwable {
    T realAnswer = (T) invocation.callRealMethod();
    return accept(realAnswer);
  }

  public T accept(T answer) {
    return spy(answer);
  }

}
