#!/usr/bin/gawk

# NB: This script uses features only found in GNU Awk!
# By using this script, you can mechanically convert the most banal usage of JUnit4 to JUnit5
# After having run this AWK script against your test sources, you will still need to do a few
# things:
#  1. Add dependencies to your POMs
#  2. Reformat all changed tests, including optimizing imports (preferably using the "Only VCS changed text" checkbox)
#  3. "Inspect code" on all changed tests, so you can replace statement lambdas with expression lambdas.
#     The name of the inspection in IntelliJ is:
#       "Statement lambda can be replaced with expression lambda"
#
# Personally, I run this script as follows (from the root of a project's git repository:
# $ git grep -lzP 'import org.junit(?!\.jupiter)' '**.java' | xargs -0 gawk -i inplace -f thisscript.awk

BEGIN {
    foundTestMethodWithExpectedException = 0;
    testMethodCurlyBracesDepth = 0;
    exceptionType = "";
}

# Add imports for stuff we might need (make sure to run a "Optimize imports" before committing)
/^package/ {
    print;
    # Annotations reside in the org.junit.jupiter.api package.
    print "import org.junit.jupiter.api.*;";
    # Assertions reside in org.junit.jupiter.api.Assertions.
    print "import static org.junit.jupiter.api.Assertions.*;";
    # @RunWith no longer exists; superseded by @ExtendWith.
    print "import org.junit.jupiter.api.extension.ExtendWith;"

    print "import org.springframework.test.context.junit.jupiter.SpringExtension;";
    print "import org.mockito.junit.jupiter.MockitoExtension;";

    next;
}

# @Before and @After no longer exist; use @BeforeEach and @AfterEach instead.
match($0, /(\s*)@(Before|After)$/, matches) {
    print matches[1] "@" matches[2] "Each";
    next;
}

# @BeforeClass and @AfterClass no longer exist; use @BeforeAll and @AfterAll instead.
match($0, /(\s*)@(Before|After)Class$/, matches) {
    print matches[1] "@" matches[2] "All";
    next;
}

# @RunWith no longer exists; superseded by @ExtendWith.
match($0, /(\s*)@RunWith\((SpringRunner|SpringJUnit4ClassRunner).class\)/, matches) {
    print matches[1] "@ExtendWith(SpringExtension.class)";
    next;
}

# @RunWith no longer exists; superseded by @ExtendWith.
match($0, /(\s*)@RunWith\(MockitoJUnitRunner.class\)/, matches) {
    print matches[1] "@ExtendWith(MockitoExtension.class)";
    next;
}

# @Ignore no longer exists: use @Disabled or one of the other built-in execution conditions instead
match($0, /(\s*)@Ignore$/, matches) {
    print matches[1] "@Disabled";
    next;
}

# Convert use of org.junit.Assert.assertThat to Hamcrest (may require extra dependency)
/^import static org.junit.Assert.assertThat;$/ {
    print "import static org.hamcrest.MatcherAssert.assertThat;";
    next;
}

# Skip static imports for assertions, as we already added a * import at the top
/^import( static)? org.junit.Assert.*;$/ {
    next;
}

# Assumptions reside in org.junit.jupiter.api.Assumptions.
match($0, /^import( static)? org.junit.Assume(.*);$/, matches) {
    print "import" matches[1] " org.junit.jupiter.api.Assumptions" matches[2] ";";
    next;
}

# Convert imports
!/jupiter/ && match($0, /^import org.junit.(.*)$/, matches) {
    print "import org.junit.jupiter.api." matches[1];
    next;
}

# Convert the use of "expected" annotation property to usage of "assertThrows"
match($0, /(\s*)@Test\s*\(\s*expected\s*=\s*([^)]+\.class)\s*\)/, matches) {
    print matches[1] "@Test"
    exceptionType = matches[2];
    foundTestMethodWithExpectedException = 1;
    next;
}

# assertThrows
/{\s*$/ && foundTestMethodWithExpectedException == 1 {
    print;
    print "assertThrows(" exceptionType ", () -> {";
    testMethodCurlyBracesDepth++;
    next;
}

# assertThrows
testMethodCurlyBracesDepth != 0 && /}\s*$/ {
    testMethodCurlyBracesDepth--;
    if (testMethodCurlyBracesDepth == 0) {
        print "});"
        foundTestMethodWithExpectedException = 0;
    }
}

# And copy whatever is left.
{
    print;
}
