package io.netlibs.ami.pump;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

import io.netlibs.ami.pump.utils.Globs;

public class EventFilter implements Predicate<String> {

  private List<NegatableFilter> filters;

  static class NegatableFilter {

    private final Predicate<String> predicate;
    private final boolean negated;

    public NegatableFilter(Predicate<String> predicate, boolean negated) {
      this.predicate = predicate;
      this.negated = negated;
    }

    public boolean test(String in) {
      return predicate.test(in);
    }

    @Override
    public String toString() {
      if (negated) {
        return "not(" + this.predicate.toString() + ")";
      }
      return this.predicate.toString();
    }

  }

  public EventFilter(List<String> filters) {
    this.filters =
      filters.stream()
        .flatMap(e -> Splitter.on(",").trimResults().omitEmptyStrings().splitToStream(e))
        .filter(e -> e.length() > 0)
        .map(e -> build(e))
        .collect(ImmutableList.toImmutableList());
  }

  private NegatableFilter build(String pattern) {

    boolean negate = false;

    if (pattern.startsWith("-")) {
      pattern = pattern.substring(1);
      negate = true;
    }

    final String input = pattern.toLowerCase();
    final String regex = Globs.toUnixRegexPattern(input);
    final Pattern matcher = Pattern.compile(regex);

    Predicate<String> predicate = new Predicate<String>() {

      final Predicate<String> inner = matcher.asMatchPredicate();

      @Override
      public boolean test(String input) {
        return inner.test(input);
      }

      @Override
      public String toString() {
        return regex + "('" + input + "')";
      }

    };

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

  @Override
  public String toString() {
    return this.filters.stream().map(e -> e.toString()).collect(Collectors.joining(", "));
  }

}
