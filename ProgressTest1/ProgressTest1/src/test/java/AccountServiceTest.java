package fe.de201028;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("AccountService")
class AccountServiceTest {

    static final String USER = "alice_01";
    static final String EMAIL = "alice@example.com";
    static final String PASS = "Secret@123";
    static final LocalDate DOB = LocalDate.of(2000, 1, 15);
    static final String PHONE = "0912345678";
    static final LocalDate CHILD_DOB = LocalDate.now().minusYears(10);

    AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountService();
    }

    void registerDefault() {
        assertEquals(
                ResultCode.SUCCESS,
                service.register(USER, EMAIL, PASS, PASS, DOB, PHONE)
        );
    }

    Account account() {
        return service.findByUsername(USER).orElseThrow();
    }

    void failLogin(int times) {
        for (int i = 0; i < times; i++) {
            service.login(USER, "Wrong@123");
        }
    }



    // =========================================================
    // TODO-5: REGISTER
    // =========================================================

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        void register_ValidData_CreatesActiveAccountWithHashedPassword() {
            ResultCode result =
                    service.register(USER, EMAIL, PASS, PASS, DOB, PHONE);

            assertEquals(ResultCode.SUCCESS, result);

            Account acc = account();

            assertEquals(AccountStatus.ACTIVE, acc.getStatus());
            assertEquals(0, acc.getFailedAttempts());
            assertFalse(acc.isLocked());

            assertNotEquals(PASS, acc.getCurrentPasswordHash());
            assertEquals(64, acc.getCurrentPasswordHash().length());
            assertEquals(1, acc.getPasswordHistory().size());
        }

        @Test
        void register_UpperCaseEmail_StoredAsLowerCase() {
            service.register(
                    USER,
                    "Alice@Example.COM",
                    PASS,
                    PASS,
                    DOB,
                    PHONE
            );

            assertEquals("alice@example.com", account().getEmail());
        }

        @Test
        void register_TwoAccountsSamePassword_HaveDifferentSaltAndHash() {
            registerDefault();

            service.register(
                    "bob_02",
                    "bob@example.com",
                    PASS,
                    PASS,
                    DOB,
                    null
            );

            Account bob =
                    service.findByUsername("bob_02").orElseThrow();

            assertNotEquals(account().getSalt(), bob.getSalt());
            assertNotEquals(
                    account().getCurrentPasswordHash(),
                    bob.getCurrentPasswordHash()
            );
        }

        // ---------------------------------------------------------
        // Input sai + thứ tự ưu tiên
        // ---------------------------------------------------------

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("fe.de201028.AccountServiceTest#invalidRegisterInputs")
        void register_InvalidInput_ReturnsExpectedCode(
                String desc,
                String username,
                String email,
                String password,
                String confirm,
                LocalDate dob,
                String phone,
                ResultCode expected) {

            ResultCode result =
                    service.register(
                            username,
                            email,
                            password,
                            confirm,
                            dob,
                            phone
                    );

            assertEquals(expected, result);

            if (username != null) {
                assertTrue(
                        service.findByUsername(username).isEmpty(),
                        "Không được tạo tài khoản"
                );
            }
        }

        // ---------------------------------------------------------
        // Username null / empty / blank
        // ---------------------------------------------------------

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void register_UsernameNullEmptyBlank_ReturnsInvalidInput(
                String username) {

            assertEquals(
                    ResultCode.INVALID_INPUT,
                    service.register(
                            username,
                            EMAIL,
                            PASS,
                            PASS,
                            DOB,
                            PHONE
                    )
            );
        }

        // ---------------------------------------------------------
        // Email null / empty / blank
        // ---------------------------------------------------------

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void register_EmailNullEmptyBlank_ReturnsInvalidInput(
                String email) {

            assertEquals(
                    ResultCode.INVALID_INPUT,
                    service.register(
                            USER,
                            email,
                            PASS,
                            PASS,
                            DOB,
                            PHONE
                    )
            );
        }

        // ---------------------------------------------------------
        // Password null / empty / blank
        // ---------------------------------------------------------

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void register_PasswordNullEmptyBlank_ReturnsInvalidInput(
                String password) {

            assertEquals(
                    ResultCode.INVALID_INPUT,
                    service.register(
                            USER,
                            EMAIL,
                            password,
                            PASS,
                            DOB,
                            PHONE
                    )
            );

            assertEquals(
                    ResultCode.INVALID_INPUT,
                    service.register(
                            USER,
                            EMAIL,
                            PASS,
                            password,
                            DOB,
                            PHONE
                    )
            );
        }

        // ---------------------------------------------------------
        // Phone null / empty được chấp nhận
        // ---------------------------------------------------------

        @ParameterizedTest
        @NullAndEmptySource
        void register_PhoneNullOrEmpty_Success(String phone) {

            assertEquals(
                    ResultCode.SUCCESS,
                    service.register(
                            USER,
                            EMAIL,
                            PASS,
                            PASS,
                            DOB,
                            phone
                    )
            );
        }

        // ---------------------------------------------------------
        // Duplicate username - không phân biệt hoa thường
        // ---------------------------------------------------------

        @ParameterizedTest
        @ValueSource(strings = {
                "alice_01",
                "ALICE_01",
                "Alice_01"
        })
        void register_DuplicateUsernameIgnoreCase_ReturnsDuplicateUsername(
                String username) {

            registerDefault();

            assertEquals(
                    ResultCode.DUPLICATE_USERNAME,
                    service.register(
                            username,
                            "other@example.com",
                            PASS,
                            PASS,
                            DOB,
                            null
                    )
            );
        }

        // ---------------------------------------------------------
        // Duplicate email - không phân biệt hoa thường
        // ---------------------------------------------------------

        @ParameterizedTest
        @ValueSource(strings = {
                "alice@example.com",
                "ALICE@EXAMPLE.COM",
                "Alice@Example.Com"
        })
        void register_DuplicateEmailIgnoreCase_ReturnsDuplicateEmail(
                String email) {

            registerDefault();

            assertEquals(
                    ResultCode.DUPLICATE_EMAIL,
                    service.register(
                            "bob_02",
                            email,
                            PASS,
                            PASS,
                            DOB,
                            null
                    )
            );

            assertTrue(
                    service.findByUsername("bob_02").isEmpty()
            );
        }

        // ---------------------------------------------------------
        // Boundary tuổi
        // ---------------------------------------------------------

        @ParameterizedTest(
                name = "[{index}] today - {0} năm + {1} ngày -> {2}"
        )
        @CsvSource({
                "18,  0, SUCCESS",
                "18,  1, UNDERAGE",
                "18, -1, SUCCESS",
                "0,   0, UNDERAGE",
                "0,   1, INVALID_INPUT"
        })
        void register_AgeBoundary(
                int yearsAgo,
                int plusDays,
                ResultCode expected) {

            LocalDate dob =
                    LocalDate.now()
                            .minusYears(yearsAgo)
                            .plusDays(plusDays);

            assertEquals(
                    expected,
                    service.register(
                            USER,
                            EMAIL,
                            PASS,
                            PASS,
                            dob,
                            null
                    )
            );
        }

        // ---------------------------------------------------------
        // Kiểm tra thứ tự ưu tiên
        // ---------------------------------------------------------

        @Test
        void register_DuplicateUsernameButInvalidEmail_ReturnsInvalidEmailFirst() {

            registerDefault();

            assertEquals(
                    ResultCode.INVALID_EMAIL,
                    service.register(
                            USER,
                            "bad-email",
                            PASS,
                            PASS,
                            DOB,
                            null
                    )
            );
        }
    }

    // =========================================================
    // MethodSource
    // =========================================================

    static Stream<Arguments> invalidRegisterInputs() {

        return Stream.of(

                // Mỗi rule một test
                Arguments.of(
                        "dob null",
                        USER,
                        EMAIL,
                        PASS,
                        PASS,
                        null,
                        PHONE,
                        ResultCode.INVALID_INPUT
                ),

                Arguments.of(
                        "username sai",
                        "1alice",
                        EMAIL,
                        PASS,
                        PASS,
                        DOB,
                        PHONE,
                        ResultCode.INVALID_USERNAME
                ),

                Arguments.of(
                        "email sai",
                        USER,
                        "alice@example",
                        PASS,
                        PASS,
                        DOB,
                        PHONE,
                        ResultCode.INVALID_EMAIL
                ),

                Arguments.of(
                        "mật khẩu yếu",
                        USER,
                        EMAIL,
                        "password",
                        "password",
                        DOB,
                        PHONE,
                        ResultCode.WEAK_PASSWORD
                ),

                Arguments.of(
                        "mật khẩu chứa username",
                        USER,
                        EMAIL,
                        "Alice_01@x",
                        "Alice_01@x",
                        DOB,
                        PHONE,
                        ResultCode.WEAK_PASSWORD
                ),

                Arguments.of(
                        "confirm lệch",
                        USER,
                        EMAIL,
                        PASS,
                        "Secret@124",
                        DOB,
                        PHONE,
                        ResultCode.PASSWORD_MISMATCH
                ),

                Arguments.of(
                        "chưa đủ tuổi",
                        USER,
                        EMAIL,
                        PASS,
                        PASS,
                        CHILD_DOB,
                        PHONE,
                        ResultCode.UNDERAGE
                ),

                Arguments.of(
                        "phone sai đầu số",
                        USER,
                        EMAIL,
                        PASS,
                        PASS,
                        DOB,
                        "0112345678",
                        ResultCode.INVALID_PHONE
                ),

                Arguments.of(
                        "phone blank",
                        USER,
                        EMAIL,
                        PASS,
                        PASS,
                        DOB,
                        "   ",
                        ResultCode.INVALID_PHONE
                ),

                // -------------------------------------------------
                // Thứ tự ưu tiên
                // -------------------------------------------------

                Arguments.of(
                        "thiếu email + username sai -> INVALID_INPUT",
                        "1alice",
                        "",
                        PASS,
                        PASS,
                        DOB,
                        PHONE,
                        ResultCode.INVALID_INPUT
                ),

                Arguments.of(
                        "username sai + email sai -> INVALID_USERNAME",
                        "1alice",
                        "bad",
                        PASS,
                        PASS,
                        DOB,
                        PHONE,
                        ResultCode.INVALID_USERNAME
                ),

                Arguments.of(
                        "email sai + mk yếu -> INVALID_EMAIL",
                        USER,
                        "bad",
                        "weak",
                        "weak",
                        DOB,
                        PHONE,
                        ResultCode.INVALID_EMAIL
                ),

                Arguments.of(
                        "mk yếu + confirm lệch -> WEAK_PASSWORD",
                        USER,
                        EMAIL,
                        "weak",
                        "other",
                        DOB,
                        PHONE,
                        ResultCode.WEAK_PASSWORD
                ),

                Arguments.of(
                        "confirm lệch + chưa đủ tuổi -> PASSWORD_MISMATCH",
                        USER,
                        EMAIL,
                        PASS,
                        "x",
                        CHILD_DOB,
                        PHONE,
                        ResultCode.PASSWORD_MISMATCH
                ),

                Arguments.of(
                        "chưa đủ tuổi + phone sai -> UNDERAGE",
                        USER,
                        EMAIL,
                        PASS,
                        PASS,
                        CHILD_DOB,
                        "123",
                        ResultCode.UNDERAGE
                )
        );
    }


    // =========================================================
    @Nested
    @DisplayName("login()")
    class Login {

        @BeforeEach
        void registerUser() {
            registerDefault();
        }

        @Test
        void login_CorrectCredentials_Success() {
            assertEquals(
                    ResultCode.SUCCESS,
                    service.login(USER, PASS)
            );

            assertEquals(
                    0,
                    account().getFailedAttempts()
            );

            assertFalse(
                    service.isLocked(USER)
            );
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        void login_UsernameNullEmptyBlank_ReturnsInvalidInput(
                String username
        ) {
            assertEquals(
                    ResultCode.INVALID_INPUT,
                    service.login(username, PASS)
            );
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        void login_PasswordNullEmptyBlank_ReturnsInvalidInput(
                String password
        ) {
            assertEquals(
                    ResultCode.INVALID_INPUT,
                    service.login(USER, password)
            );

            assertEquals(
                    0,
                    account().getFailedAttempts()
            );
        }

        @ParameterizedTest(name = "[{index}] username \"{0}\"")
        @ValueSource(strings = {
                "alice_01",
                "ALICE_01",
                "Alice_01"
        })
        void login_UsernameIgnoreCase_Success(
                String username
        ) {
            assertEquals(
                    ResultCode.SUCCESS,
                    service.login(username, PASS)
            );
        }

        @ParameterizedTest(name = "[{index}] password \"{0}\"")
        @ValueSource(strings = {
                "secret@123",
                "SECRET@123"
        })
        void login_PasswordCaseSensitive_ReturnsInvalidCredentials(
                String password
        ) {
            assertEquals(
                    ResultCode.INVALID_CREDENTIALS,
                    service.login(USER, password)
            );
        }

        @Test
        void login_UnknownUserAndWrongPassword_ReturnSameCode() {

            assertEquals(
                    ResultCode.INVALID_CREDENTIALS,
                    service.login("nobody_1", PASS)
            );

            assertEquals(
                    ResultCode.INVALID_CREDENTIALS,
                    service.login(USER, "Wrong@123")
            );
        }

        @ParameterizedTest(name = "[{index}] sai {0} lần -> chưa khóa")
        @ValueSource(ints = {
                1, 2, 3, 4
        })
        void login_WrongPasswordLessThan5Times_IncrementsCounter(
                int times
        ) {
            failLogin(times - 1);

            ResultCode result =
                    service.login(USER, "Wrong@123");

            assertEquals(
                    ResultCode.INVALID_CREDENTIALS,
                    result
            );

            assertEquals(
                    times,
                    account().getFailedAttempts()
            );

            assertFalse(
                    service.isLocked(USER)
            );
        }

        @Test
        void login_WrongPassword5thTime_LocksAccount() {

            failLogin(4);

            ResultCode result =
                    service.login(USER, "Wrong@123");

            assertEquals(
                    ResultCode.ACCOUNT_LOCKED,
                    result
            );

            assertEquals(
                    5,
                    account().getFailedAttempts()
            );

            assertTrue(
                    service.isLocked(USER)
            );
        }

        @ParameterizedTest(name = "[{index}] đang khóa + password \"{0}\"")
        @ValueSource(strings = {
                PASS,
                "Wrong@123"
        })
        void login_WhileLocked_RejectsWithoutIncrement(
                String password
        ) {
            failLogin(5);

            assertEquals(
                    ResultCode.ACCOUNT_LOCKED,
                    service.login(USER, password)
            );

            assertEquals(
                    5,
                    account().getFailedAttempts()
            );

            assertTrue(
                    service.isLocked(USER)
            );
        }

        @ParameterizedTest(name = "[{index}] {0} lần sai -> {1}, locked={2}")
        @CsvSource({
                "3, SUCCESS,        false",
                "4, SUCCESS,        false",
                "5, ACCOUNT_LOCKED, true",
                "6, ACCOUNT_LOCKED, true"
        })
        void login_CorrectPasswordAfterNFailures(
                int failures,
                ResultCode expected,
                boolean locked
        ) {
            failLogin(failures);

            assertEquals(
                    expected,
                    service.login(USER, PASS)
            );

            assertEquals(
                    locked,
                    service.isLocked(USER)
            );
        }

        @Test
        void login_AfterAdminUnlock_CounterRestartsAndCanLogin() {

            failLogin(5);

            assertEquals(
                    ResultCode.SUCCESS,
                    service.unlockAccount(USER)
            );

            assertFalse(
                    service.isLocked(USER)
            );

            assertEquals(
                    0,
                    account().getFailedAttempts()
            );

            assertEquals(
                    ResultCode.INVALID_CREDENTIALS,
                    service.login(USER, "Wrong@123")
            );

            assertEquals(
                    1,
                    account().getFailedAttempts()
            );

            assertEquals(
                    ResultCode.SUCCESS,
                    service.login(USER, PASS)
            );
        }

        @Test
        void login_SuccessAfterFailures_ResetsCounter() {

            failLogin(3);

            assertEquals(
                    ResultCode.SUCCESS,
                    service.login(USER, PASS)
            );

            assertEquals(
                    0,
                    account().getFailedAttempts()
            );
        }

        @ParameterizedTest(name = "[{index}] DISABLED + password \"{0}\"")
        @ValueSource(strings = {
                PASS,
                 "Wrong@123"
        })
        void login_DisabledAccount_ReturnsAccountDisabled(
                String password
        ) {
            service.disableAccount(USER);

            assertEquals(
                    ResultCode.ACCOUNT_DISABLED,
                    service.login(USER, password)
            );

            assertEquals(
                    0,
                    account().getFailedAttempts()
            );
        }
    }

    // =========================================================
    @Nested
    @DisplayName("Quản trị & truy vấn")
    class Admin {

        @Test
        void disableAccount_ExistingUser_SetsDisabled() {

            registerDefault();

            assertEquals(
                    ResultCode.SUCCESS,
                    service.disableAccount("ALICE_01")
            );

            assertEquals(
                    AccountStatus.DISABLED,
                    account().getStatus()
            );
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
                " ",
                "nobody_1"
        })
        void disableAccount_BlankOrUnknown_ReturnsUserNotFound(
                String username
        ) {
            assertEquals(
                    ResultCode.USER_NOT_FOUND,
                    service.disableAccount(username)
            );
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
                " ",
                "nobody_1"
        })
        void unlockAccount_BlankOrUnknown_ReturnsUserNotFound(
                String username
        ) {
            assertEquals(
                    ResultCode.USER_NOT_FOUND,
                    service.unlockAccount(username)
            );
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
                " ",
                "nobody_1"
        })
        void findByUsername_BlankOrUnknown_ReturnsEmpty(
                String username
        ) {
            assertTrue(
                    service.findByUsername(username).isEmpty()
            );

            assertFalse(
                    service.isLocked(username)
            );
        }
    }
}