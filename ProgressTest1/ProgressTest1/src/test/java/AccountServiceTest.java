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
}