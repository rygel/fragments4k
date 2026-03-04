Supported
TypeScript, JavaScript, Java, Kotlin, C#, Go, Rust, Markdown

Use Cases
Codebase structure exploration
Finding classes/functions
Code review preparation
API endpoint discovery

## Maven Surefire Best Practices ⚠️

**CRITICAL: Always ensure tests compile AND execute successfully with Maven Surefire.**

### Common Surefire Issues in This Project

1. **Kotlin Test Method Names**: 
   - NEVER use backticks (`` ` ``) in test method names
   - BAD: `` fun `test something`() ``
   - GOOD: `fun testSomething()`
   - Surefire 3.3.0 parses backticks as parameter delimiters causing execution failures

2. **Test Compilation vs Execution**:
   - `mvn test-compile` only checks compilation
   - `mvn test` runs the actual tests
   - Always verify both steps before considering work complete

3. **Special Characters to Avoid**:
   - Backticks (`` ` ``)
   - Exclamation marks (`!`)
   - Parentheses in method names
   - Any non-alphanumeric characters except underscore

4. **Verification Steps**:
   ```bash
   # 1. Compile tests
   mvn test-compile
   
   # 2. Run tests
   mvn test
   
   # 3. Check specific module
   mvn -pl fragments-core test
   ```

5. **When Tests Fail to Execute**:
   - Check for parameter parsing errors in output
   - Look for method names with special characters
   - Verify Surefire version (3.3.0 has known issues)
   - Run with `-X` flag for detailed diagnostics

### Pre-Commit Checklist
- [ ] `mvn test-compile` passes
- [ ] `mvn test` passes (or at least executes without Surefire errors)
- [ ] All test method names use camelCase only
- [ ] No backticks in test method names
