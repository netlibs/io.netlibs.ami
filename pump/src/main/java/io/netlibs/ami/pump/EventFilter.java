package io.netlibs.ami.pump;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

import io.netlibs.ami.pump.utils.Globs;

public class EventFilter implements Predicate<String> {

  private List<NegatableFilter> filters;

  static class NegatableFilter {

    private Predicate<String> predicate;
    private boolean negated;

    public NegatableFilter(Predicate<String> predicate, boolean negated) {
      this.predicate = predicate;
      this.negated = negated;
    }

    public boolean test(String in) {
      return predicate.test(in);
    }

  }

  public EventFilter(List<String> filters) {
    this.filters =
      filters.stream()
        .flatMap(e -> Splitter.on(",").trimResults().omitEmptyStrings().splitToStream(e))
        .map(e -> build(e))
        .collect(ImmutableList.toImmutableList());
  }

  private NegatableFilter build(String pattern) {

    boolean negate = false;

    if (pattern.startsWith("-")) {
      pattern = pattern.substring(1);
      negate = true;
    }

    Pattern matcher = Pattern.compile(Globs.toUnixRegexPattern(pattern.toLowerCase()));

    Predicate<String> predicate = matcher.asPredicate();

    if (negate) {
      return new NegatableFilter(predicate, true);
    }

    return new NegatableFilter(predicate, false);

  }

  @Override
  public boolean test(String in) {
    in = in.toLowerCase();
    boolean match = false;
    for (NegatableFilter filter : filters) {
      if (filter.test(in)) {
        match = true;
        if (filter.negated) {
          return false;
        }
      }
    }
    return match;
  }

}
