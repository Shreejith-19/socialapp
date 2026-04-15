/**
 * DEPLOYMENT CHECKLIST FOR PRODUCTION
 * ====================================
 * 
 * Use this checklist to verify the frontend is ready for production deployment.
 * Check all items before deploying to production environment.
 * 
 * 
 * A. PRE-DEPLOYMENT VERIFICATION
 * ===============================
 * 
 * Code Quality:
 * ☐ All compilation warnings resolved: mvn clean compile
 * ☐ No unused imports in any Java file
 * ☐ No TODO/FIXME comments left in code
 * ☐ All exception handling implemented (no bare catch blocks)
 * ☐ No hardcoded passwords or secrets in code
 * ☐ No test-only code left in production files
 * ☐ Code follows project conventions
 * 
 * Configuration:
 * ☐ BASE_URL in RestApiClient points to production backend
 *    File: frontend/service/RestApiClient.java, line ~17
 *    Current: private static final String BASE_URL = "http://localhost:8080/api/v1";
 *    Should be: private static final String BASE_URL = "https://yourserver.com/api/v1";
 * ☐ All hardcoded test values removed
 * ☐ Logging level set appropriately (not DEBUG)
 * ☐ No console output left for debugging
 * 
 * Dependencies:
 * ☐ All Maven dependencies have valid versions
 * ☐ No SNAPSHOT versions used (mvn dependency:list | grep SNAPSHOT)
 * ☐ Security vulnerability scan passed (mvn dependency-check:check)
 * ☐ All dependencies are in secure repository (not compromised)
 * 
 * Build Process:
 * ☐ Clean build succeeds: mvn clean package
 * ☐ All tests pass: mvn test
 * ☐ Integration tests pass (if any): mvn verify
 * ☐ No build warnings or errors
 * ☐ JAR file is created successfully
 * 
 * 
 * B. SECURITY VERIFICATION
 * =========================
 * 
 * Authentication:
 * ☐ JWT token is stored securely (static field, cleared on logout)
 * ☐ No token is logged or printed in console
 * ☐ Token is always cleared after logout
 * ☐ Token is cleared on 401 (session expired)
 * ☐ No hardcoded demo tokens in code
 * 
 * HTTPS/TLS:
 * ☐ Production BASE_URL uses HTTPS (not HTTP)
 * ☐ Backend has valid SSL certificate
 * ☐ Certificate is trusted (not self-signed in production)
 * ☐ TLS 1.2 or higher is configured
 * 
 * Data Protection:
 * ☐ Sensitive data is not logged
 * ☐ Error messages don't expose internal system details
 * ☐ Stack traces are caught and shown user-friendly messages
 * ☐ No personally identifiable information (PII) in logs
 * 
 * Input Validation:
 * ☐ All user input is validated before sending to backend
 * ☐ Text fields have character limits (verified in FXML)
 * ☐ Numeric fields only accept numbers (TextFormatter in controllers)
 * ☐ Email fields validate email format
 * ☐ Passwords are masked in UI
 * 
 * Access Control:
 * ☐ Admin panel only accessible to users with ADMIN role
 * ☐ Moderation dashboard only accessible to MODERATOR/ADMIN
 * ☐ Appeal screen only shown to banned users
 * ☐ All role checks use RoleManager (centralized)
 * ☐ Permission denied errors have user-friendly messages
 * 
 * 
 * C. FUNCTIONAL VERIFICATION
 * ===========================
 * 
 * Authentication:
 * ☐ Login with valid credentials succeeds
 * ☐ Login with invalid credentials shows appropriate error
 * ☐ Session persists across navigation
 * ☐ Logout clears session completely
 * ☐ Cannot access authenticated screens after logout
 * ☐ Session expired (401) redirects to login
 * 
 * User Dashboard:
 * ☐ Feed loads posts correctly
 * ☐ Posts show author information
 * ☐ Like/dislike functionality works
 * ☐ Post creation succeeds
 * ☐ Post creation validation works
 * ☐ Pagination works (next/previous)
 * ☐ Cannot create post if banned (shows appropriate error)
 * 
 * Role-Based Features:
 * ☐ Regular users see appropriate UI
 * ☐ Moderators see moderation button
 * ☐ Admins see admin and moderation buttons
 * ☐ Non-authorized users cannot access restricted features
 * ☐ Role buttons appear/disappear based on user role
 * 
 * Moderation (if user is MODERATOR/ADMIN):
 * ☐ Moderation dashboard loads flagged posts
 * ☐ Can view post details
 * ☐ Can submit moderation decision (approve/remove/escalate)
 * ☐ Decision persists after reload
 * ☐ Non-moderators cannot access
 * 
 * Admin Panel (if user is ADMIN):
 * ☐ User management tab shows all users
 * ☐ Can ban user (user.enabled = false)
 * ☐ Ban reason is captured
 * ☐ Can unban user (user.enabled = true)
 * ☐ Appeals tab shows pending appeals
 * ☐ Can approve appeal
 * ☐ Can reject appeal with reason
 * ☐ Approved appeal unbans user
 * ☐ Non-admins cannot access
 * 
 * Appeal Screen:
 * ☐ Banned users see appeal button
 * ☐ Ban reason is displayed
 * ☐ Can submit appeal with reason
 * ☐ Validation enforces character limits (10-1000)
 * ☐ Success message appears after submission
 * ☐ Previous appeals are displayed with status
 * ☐ Can navigate back to feed
 * 
 * Error Handling:
 * ☐ 401 (Session Expired) shows proper message, redirects to login
 * ☐ 403 (Access Denied) shows proper message
 * ☐ 403 with ban shows ban reason and appeal button
 * ☐ 404 (Not Found) shows appropriate message
 * ☐ 5xx (Server Error) shows appropriate message
 * ☐ Network errors show connection error message
 * ☐ All error messages are user-friendly (no stack traces)
 * 
 * 
 * D. PERFORMANCE VERIFICATION
 * ===========================
 * 
 * Load Times:
 * ☐ Application starts in < 5 seconds
 * ☐ Feed loads in < 2 seconds (on production network)
 * ☐ Admin panel loads users in < 3 seconds
 * ☐ Other screens load in < 2 seconds
 * ☐ No noticeable lag when navigating screens
 * 
 * Memory Usage:
 * ☐ Initial memory footprint acceptable (< 200MB)
 * ☐ Memory doesn't grow significantly during use
 * ☐ No memory leaks detected (after 1 hour of use)
 * ☐ Can handle pagination without memory issues
 * 
 * Network:
 * ☐ API responses are cached where appropriate
 * ☐ Unnecessary API calls are avoided
 * ☐ Pagination reduces data transfer
 * ☐ Error recovery doesn't cause excessive retries
 * 
 * 
 * E. COMPATIBILITY VERIFICATION
 * ==============================
 * 
 * Java Version:
 * ☐ Built with Java 21 (or target version)
 * ☐ Users have Java 21 JRE installed
 * ☐ No deprecated Java features used
 * ☐ No Java 8/11/17 specific issues
 * 
 * JavaFX:
 * ☐ Application runs on JavaFX 21
 * ☐ All FXML files are valid
 * ☐ All stylesheets (CSS) are valid
 * ☐ Theming works correctly
 * 
 * Operating System:
 * ☐ Tested on Windows (primary platform)
 * ☐ Tested on Linux (if deploying there)
 * ☐ Tested on macOS (if deploying there)
 * ☐ No OS-specific issues found
 * 
 * Screen Resolutions:
 * ☐ Tested on 1920x1080 (Full HD)
 * ☐ Tested on 1366x768 (Common laptop)
 * ☐ Tested on 2560x1440 (2K)
 * ☐ UI scales appropriately
 * ☐ No controls cut off at different resolutions
 * 
 * Backend Compatibility:
 * ☐ Backend implements all required endpoints
 * ☐ Backend returns JWT tokens in login response
 * ☐ Backend validates JWT in Authorization header
 * ☐ Backend returns correct HTTP status codes (401, 403, 500, etc.)
 * ☐ Backend returns DTOs matching Model layer
 * ☐ Backend supports pagination format
 * 
 * 
 * F. DOCUMENTATION VERIFICATION
 * ==============================
 * 
 * User Documentation:
 * ☐ README.md describes how to run the application
 * ☐ Setup instructions are clear and complete
 * ☐ Any custom configuration is documented
 * ☐ Troubleshooting section covers common issues
 * 
 * Developer Documentation:
 * ☐ INTEGRATION_VERIFICATION.md describes architecture
 * ☐ JWT_INTEGRATION_GUIDE.md explains how JWT works
 * ☐ Code has Javadoc comments on public methods
 * ☐ Complex logic is commented
 * ☐ FXML files are reasonably readable
 * 
 * Deployment Documentation:
 * ☐ Clear instructions for setting up production backend
 * ☐ Instructions for building application
 * ☐ Instructions for running application
 * ☐ Instructions for updating BASE_URL for new backend
 * ☐ Troubleshooting guide for common issues
 * 
 * 
 * G. BACKUP & RECOVERY
 * ====================
 * 
 * Version Control:
 * ☐ All code committed to git
 * ☐ Production-ready state tagged in git
 * ☐ Previous versions accessible in git history
 * ☐ Build artifacts excluded from git (.gitignore)
 * 
 * Build Artifacts:
 * ☐ Production JAR built and stored securely
 * ☐ Build process documented and repeatable
 * ☐ Can rebuild from source at any time
 * ☐ Version number in JAR manifest (if applicable)
 * 
 * Documentation:
 * ☐ All documentation committed to versioning system
 * ☐ Configuration examples preserved
 * ☐ Deployment instructions versioned
 * 
 * 
 * H. DEPLOYMENT STEPS
 * ===================
 * 
 * Before Deployment:
 * 1. ☐ Complete all checks above
 * 2. ☐ Create backup of current production
 * 3. ☐ Notify users of deployment (if scheduled)
 * 4. ☐ Have rollback plan ready
 * 
 * During Deployment:
 * 1. ☐ Stop current application (if running)
 * 2. ☐ Backup old version
 * 3. ☐ Deploy new JAR
 * 4. ☐ Start new application
 * 5. ☐ Verify startup successful
 * 6. ☐ Run smoke tests (login, load feed, etc.)
 * 7. ☐ Notify users deployment complete
 * 
 * After Deployment:
 * 1. ☐ Monitor application logs for errors
 * 2. ☐ Monitor performance metrics
 * 3. ☐ Verify no user-facing issues
 * 4. ☐ Collect feedback from users
 * 5. ☐ Have rollback ready for 1 hour in case of critical issues
 * 
 * Rollback (if needed):
 * 1. ☐ Stop new version
 * 2. ☐ Restore previous version from backup
 * 3. ☐ Start previous version
 * 4. ☐ Verify rollback successful
 * 5. ☐ Investigate issue
 * 6. ☐ Fix and re-deploy
 * 
 * 
 * I. POST-DEPLOYMENT VALIDATION
 * ==============================
 * 
 * Immediate (First 5 minutes):
 * ☐ Application starts without errors
 * ☐ Health check endpoint responds (if available)
 * ☐ Login screen loads
 * ☐ Can successfully login with test account
 * 
 * Short-term (First hour):
 * ☐ No error messages in logs
 * ☐ Dashboard loads posts successfully
 * ☐ All navigation buttons work
 * ☐ User roles working correctly
 * ☐ Error handling works (test 401, 403, network error)
 * 
 * Medium-term (First day):
 * ☐ No memory leaks detected
 * ☐ Performance acceptable
 * ☐ No unusual error patterns
 * ☐ User feedback positive
 * ☐ Admin panel working (if tested)
 * ☐ Moderation features working (if tested)
 * 
 * Long-term (First week):
 * ☐ No crashes or hangs
 * ☐ Database connections stable
 * ☐ API integration reliable
 * ☐ User adoption smooth
 * ☐ Any bugs identified and logged
 * 
 * 
 * J. CONFIGURATION FOR DIFFERENT ENVIRONMENTS
 * =============================================
 * 
 * Development:
 * BASE_URL = "http://localhost:8080/api/v1"
 * LOG_LEVEL = DEBUG
 * SHOW_STACK_TRACES = true
 * 
 * Staging:
 * BASE_URL = "https://staging-api.example.com/api/v1"
 * LOG_LEVEL = INFO
 * SHOW_STACK_TRACES = false
 * 
 * Production:
 * BASE_URL = "https://api.example.com/api/v1"
 * LOG_LEVEL = WARN
 * SHOW_STACK_TRACES = false
 * 
 * To switch environments:
 * 1. Update BASE_URL in RestApiClient.java
 * 2. Ensure backend is running at new URL
 * 3. Clear any cached data (browser cache, cookies, etc.)
 * 4. Rebuild and test
 * 
 * 
 * K. EMERGENCY CONTACTS & ESCALATION
 * ===================================
 * 
 * Critical Issues:
 * - Application won't start: Check Java version, check BASE_URL
 * - Login fails: Check backend availability, check network
 * - Users get logged out: Check JWT token expiration on backend
 * - Memory errors: Check pagination, restart application
 * 
 * Contact Points:
 * ☐ Backend team contact for API issues
 * ☐ Infrastructure team contact for deployment issues
 * ☐ Security team contact for security issues
 * ☐ Product team contact for feature issues
 * 
 * 
 * SIGN-OFF
 * ========
 * 
 * Prepared by: ___________________________  Date: __________
 * Reviewed by: ___________________________  Date: __________
 * Approved by: ___________________________  Date: __________
 * 
 * Deployment completed on: ___________________ at ___:___ ___
 * Deployed to: _______________________________
 * By: _______________________________________
 * 
 * Any issues encountered:
 * _________________________________________________________________
 * _________________________________________________________________
 * _________________________________________________________________
 * 
 */
