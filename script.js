/* =====================================================
   أكاديمية المنى
   النظام الرئيسي للموقع
===================================================== */

const STUDENTS_KEY = "academy_students";
const OWNER_SESSION = "academy_owner_logged_in";


/* =====================================================
   أدوات عامة
===================================================== */

function getStudents() {

    try {

        return JSON.parse(
            localStorage.getItem(STUDENTS_KEY)
        ) || [];

    } catch (error) {

        return [];

    }

}


function saveStudents(students) {

    localStorage.setItem(
        STUDENTS_KEY,
        JSON.stringify(students)
    );

}


function showToast(message) {

    let toast = document.querySelector(".toast");

    if (!toast) {

        toast = document.createElement("div");

        toast.className = "toast";

        document.body.appendChild(toast);

    }

    toast.textContent = message;

    toast.classList.add("show");

    clearTimeout(window.toastTimer);

    window.toastTimer = setTimeout(() => {

        toast.classList.remove("show");

    }, 3000);

}


/* =====================================================
   القائمة في الهاتف
===================================================== */

function initNavigation() {

    const toggle =
        document.querySelector(".nav-toggle");

    const nav =
        document.querySelector(".main-nav");

    if (!toggle || !nav) return;

    toggle.addEventListener("click", () => {

        nav.classList.toggle("open");

    });

}


/* =====================================================
   عداد الطلاب
===================================================== */

function updateStudentCounters() {

    const students = getStudents();

    const homeCounter =
        document.getElementById("student-count");

    const adminCounter =
        document.getElementById("admin-student-count");

    if (homeCounter) {

        homeCounter.textContent =
            students.length;

    }

    if (adminCounter) {

        adminCounter.textContent =
            students.length;

    }

    const lastStudent =
        document.getElementById("last-student");

    if (lastStudent && students.length) {

        lastStudent.textContent =
            students[students.length - 1].studentName;

    }

}


/* =====================================================
   تسجيل الطالب
===================================================== */

function initStudentRegistration() {

    const form =
        document.getElementById(
            "student-register-form"
        );

    if (!form) return;


    form.addEventListener("submit", function(event) {

        event.preventDefault();


        const studentName =
            document.getElementById("student-name").value.trim();

        const studentAge =
            document.getElementById("student-age").value.trim();

        const studentGender =
            document.getElementById("student-gender").value;

        const studentClass =
            document.getElementById("student-class").value.trim();


        const parentName =
            document.getElementById("parent-name").value.trim();

        const phone =
            document.getElementById("phone").value.trim();

        const extraPhone =
            document.getElementById("extra-phone").value.trim();

        const relation =
            document.getElementById("relation").value;


        const governorate =
            document.getElementById("governorate").value.trim();

        const area =
            document.getElementById("area").value.trim();

        const address =
            document.getElementById("address").value.trim();

        const notes =
            document.getElementById("notes").value.trim();


        const confirmed =
            document.getElementById("confirm-data").checked;


        /* =============================
           التحقق
        ============================= */

        if (!studentName) {

            showToast("يرجى إدخال اسم الطالب");

            return;

        }


        if (
            !studentAge ||
            Number(studentAge) < 3 ||
            Number(studentAge) > 30
        ) {

            showToast("يرجى إدخال عمر صحيح");

            return;

        }


        if (!studentGender) {

            showToast("يرجى اختيار النوع");

            return;

        }


        if (!studentClass) {

            showToast("يرجى إدخال الصف الدراسي");

            return;

        }


        if (!parentName) {

            showToast("يرجى إدخال اسم ولي الأمر");

            return;

        }


        if (!phone) {

            showToast("يرجى إدخال رقم الهاتف");

            return;

        }


        if (!relation) {

            showToast("يرجى اختيار صلة القرابة");

            return;

        }


        if (!governorate || !area || !address) {

            showToast(
                "يرجى إكمال بيانات السكن"
            );

            return;

        }


        if (!confirmed) {

            showToast(
                "يجب الموافقة على صحة البيانات"
            );

            return;

        }


        /* =============================
           إنشاء الطالب
        ============================= */

        const student = {

            id: Date.now(),

            studentName,

            studentAge,

            studentGender,

            studentClass,

            parentName,

            phone,

            extraPhone,

            relation,

            governorate,

            area,

            address,

            notes,

            createdAt:
                new Date().toISOString()

        };


        const students = getStudents();

        students.push(student);

        saveStudents(students);


        /* =============================
           رسالة واتساب
        ============================= */

        const message =

`🎓 *طلب تسجيل طالب جديد*
*أكاديمية المنى*

━━━━━━━━━━━━━━━━

👨‍🎓 *بيانات الطالب*

الاسم: ${studentName}
العمر: ${studentAge}
النوع: ${studentGender}
الصف الدراسي: ${studentClass}

━━━━━━━━━━━━━━━━

👨‍👩‍👦 *بيانات ولي الأمر*

الاسم: ${parentName}
صلة القرابة: ${relation}
الهاتف الأساسي: ${phone}
رقم إضافي: ${extraPhone || "غير موجود"}

━━━━━━━━━━━━━━━━

📍 *بيانات السكن*

المحافظة: ${governorate}
المنطقة / المركز: ${area}
العنوان: ${address}

━━━━━━━━━━━━━━━━

📝 *ملاحظات*

${notes || "لا توجد ملاحظات"}

━━━━━━━━━━━━━━━━

✅ تم إرسال الطلب من موقع أكاديمية المنى.`;


        const whatsappNumber =
            "201140752330";


        const whatsappURL =
            "https://wa.me/" +
            whatsappNumber +
            "?text=" +
            encodeURIComponent(message);


        showToast(
            "تم حفظ الطلب، جاري فتح واتساب..."
        );


        setTimeout(() => {

            window.location.href =
                whatsappURL;

        }, 700);

    });

}


/* =====================================================
   تسجيل دخول المالك
===================================================== */

/*
   غيّر بيانات الدخول هنا.
   
   اسم المستخدم:
   owner
   
   كلمة المرور:
   Almona@2026
*/

const OWNER_USERNAME = "owner";
const OWNER_PASSWORD = "Almona@2026";


function initOwnerLogin() {

    const form =
        document.getElementById(
            "owner-login-form"
        );

    if (!form) return;


    form.addEventListener("submit", function(event) {

        event.preventDefault();


        const username =
            document.getElementById(
                "owner-username"
            ).value.trim();


        const password =
            document.getElementById(
                "owner-password"
            ).value;


        const error =
            document.getElementById(
                "login-error"
            );


        if (
            username === OWNER_USERNAME &&
            password === OWNER_PASSWORD
        ) {

            localStorage.setItem(
                OWNER_SESSION,
                "true"
            );


            showToast(
                "تم تسجيل الدخول بنجاح"
            );


            setTimeout(() => {

                window.location.href =
                    "admin.html";

            }, 700);


        } else {

            if (error) {

                error.classList.add("show");

            }

            showToast(
                "اسم المستخدم أو كلمة المرور غير صحيحة"
            );

        }

    });

}


/* =====================================================
   حماية لوحة الإدارة
===================================================== */

function protectAdminPage() {

    const isAdminPage =
        document.body.classList.contains(
            "admin-page"
        );

    if (!isAdminPage) return;


    const loggedIn =
        localStorage.getItem(
            OWNER_SESSION
        ) === "true";


    if (!loggedIn) {

        window.location.href =
            "login.html";

    }

}


/* =====================================================
   تسجيل الخروج
===================================================== */

function initLogout() {

    const button =
        document.getElementById(
            "logout-btn"
        );

    if (!button) return;


    button.addEventListener("click", () => {

        localStorage.removeItem(
            OWNER_SESSION
        );

        window.location.href =
            "login.html";

    });

}


/* =====================================================
   رسم جدول الطلاب
===================================================== */

function renderStudents(search = "") {

    const tableBody =
        document.getElementById(
            "students-table-body"
        );

    const empty =
        document.getElementById(
            "empty-students"
        );

    if (!tableBody) return;


    const students =
        getStudents();


    const query =
        search.trim().toLowerCase();


    const filtered =
        students.filter(student => {

            const text = (

                student.studentName +
                " " +
                student.parentName +
                " " +
                student.phone +
                " " +
                student.studentClass +
                " " +
                student.governorate

            ).toLowerCase();


            return text.includes(query);

        });


    tableBody.innerHTML = "";


    if (!filtered.length) {

        if (empty) {

            empty.style.display =
                "block";

        }

        return;

    }


    if (empty) {

        empty.style.display =
            "none";

    }


    filtered.forEach((student, index) => {

        const row =
            document.createElement("tr");


        row.innerHTML = `

            <td>
                ${index + 1}
            </td>

            <td>
                <strong>
                    ${escapeHTML(student.studentName)}
                </strong>

                <small>
                    ${escapeHTML(student.studentGender)}
                </small>
            </td>

            <td>
                ${escapeHTML(student.studentAge)}
            </td>

            <td>
                ${escapeHTML(student.studentClass)}
            </td>

            <td>
                ${escapeHTML(student.parentName)}
            </td>

            <td>
                ${escapeHTML(student.phone)}
            </td>

            <td>
                ${escapeHTML(student.governorate)}
            </td>

            <td>

                <div class="table-actions">

                    <button
                        class="action-edit"
                        data-id="${student.id}">
                        تعديل
                    </button>

                    <button
                        class="action-delete"
                        data-id="${student.id}">
                        حذف
                    </button>

                </div>

            </td>

        `;


        tableBody.appendChild(row);

    });


    bindStudentActions();

}


/* =====================================================
   حماية النصوص داخل HTML
===================================================== */

function escapeHTML(value) {

    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");

}


/* =====================================================
   أزرار تعديل وحذف
===================================================== */

function bindStudentActions() {

    document
        .querySelectorAll(".action-delete")
        .forEach(button => {

            button.addEventListener(
                "click",
                () => {

                    const id =
                        Number(
                            button.dataset.id
                        );


                    const students =
                        getStudents();


                    const student =
                        students.find(
                            item => item.id === id
                        );


                    if (!student) return;


                    const confirmed =
                        confirm(
                            `هل تريد حذف الطالب "${student.studentName}"؟`
                        );


                    if (!confirmed) return;


                    const updated =
                        students.filter(
                            item => item.id !== id
                        );


                    saveStudents(updated);

                    renderStudents();

                    updateStudentCounters();

                    showToast(
                        "تم حذف الطالب"
                    );

                }
            );

        });


    document
        .querySelectorAll(".action-edit")
        .forEach(button => {

            button.addEventListener(
                "click",
                () => {

                    openEditModal(
                        Number(
                            button.dataset.id
                        )
                    );

                }
            );

        });

}


/* =====================================================
   نافذة تعديل الطالب
===================================================== */

function openEditModal(id) {

    const students =
        getStudents();


    const student =
        students.find(
            item => item.id === id
        );


    if (!student) return;


    document.getElementById("edit-id").value =
        student.id;

    document.getElementById("edit-name").value =
        student.studentName;

    document.getElementById("edit-age").value =
        student.studentAge;

    document.getElementById("edit-gender").value =
        student.studentGender;

    document.getElementById("edit-class").value =
        student.studentClass;

    document.getElementById("edit-parent").value =
        student.parentName;

    document.getElementById("edit-phone").value =
        student.phone;

    document.getElementById("edit-extra-phone").value =
        student.extraPhone || "";

    document.getElementById("edit-relation").value =
        student.relation;

    document.getElementById("edit-governorate").value =
        student.governorate;

    document.getElementById("edit-area").value =
        student.area;

    document.getElementById("edit-address").value =
        student.address;

    document.getElementById("edit-notes").value =
        student.notes || "";


    const modal =
        document.getElementById(
            "edit-modal"
        );


    if (modal) {

        modal.classList.add("show");

    }

}


/* =====================================================
   حفظ تعديل الطالب
===================================================== */

function initEditForm() {

    const form =
        document.getElementById(
            "edit-student-form"
        );

    if (!form) return;


    form.addEventListener("submit", event => {

        event.preventDefault();


        const id =
            Number(
                document.getElementById(
                    "edit-id"
                ).value
            );


        const students =
            getStudents();


        const index =
            students.findIndex(
                item => item.id === id
            );


        if (index === -1) return;


        students[index] = {

            ...students[index],

            studentName:
                document.getElementById(
                    "edit-name"
                ).value.trim(),

            studentAge:
                document.getElementById(
                    "edit-age"
                ).value.trim(),

            studentGender:
                document.getElementById(
                    "edit-gender"
                ).value,

            studentClass:
                document.getElementById(
                    "edit-class"
                ).value.trim(),

            parentName:
                document.getElementById(
                    "edit-parent"
                ).value.trim(),

            phone:
                document.getElementById(
                    "edit-phone"
                ).value.trim(),

            extraPhone:
                document.getElementById(
                    "edit-extra-phone"
                ).value.trim(),

            relation:
                document.getElementById(
                    "edit-relation"
                ).value.trim(),

            governorate:
                document.getElementById(
                    "edit-governorate"
                ).value.trim(),

            area:
                document.getElementById(
                    "edit-area"
                ).value.trim(),

            address:
                document.getElementById(
                    "edit-address"
                ).value.trim(),

            notes:
                document.getElementById(
                    "edit-notes"
                ).value.trim()

        };


        saveStudents(students);

        closeEditModal();

        renderStudents();

        updateStudentCounters();

        showToast(
            "تم حفظ تعديلات الطالب"
        );

    });

}


/* =====================================================
   إغلاق نافذة التعديل
===================================================== */

function closeEditModal() {

    const modal =
        document.getElementById(
            "edit-modal"
        );

    if (modal) {

        modal.classList.remove("show");

    }

}


function initModal() {

    const close =
        document.getElementById(
            "close-modal"
        );


    if (close) {

        close.addEventListener(
            "click",
            closeEditModal
        );

    }


    const modal =
        document.getElementById(
            "edit-modal"
        );


    if (modal) {

        modal.addEventListener(
            "click",
            event => {

                if (
                    event.target === modal
                ) {

                    closeEditModal();

                }

            }
        );

    }

}


/* =====================================================
   البحث
===================================================== */

function initStudentSearch() {

    const search =
        document.getElementById(
            "student-search"
        );

    if (!search) return;


    search.addEventListener(
        "input",
        () => {

            renderStudents(
                search.value
            );

        }
    );

}


/* =====================================================
   حذف جميع الطلاب
===================================================== */

function initClearStudents() {

    const button =
        document.getElementById(
            "clear-students"
        );

    if (!button) return;


    button.addEventListener(
        "click",
        () => {

            const students =
                getStudents();


            if (!students.length) {

                showToast(
                    "لا توجد بيانات لحذفها"
                );

                return;

            }


            const confirmed =
                confirm(
                    "تحذير: سيتم حذف جميع الطلاب نهائياً من هذا الجهاز. هل تريد المتابعة؟"
                );


            if (!confirmed) return;


            localStorage.removeItem(
                STUDENTS_KEY
            );


            renderStudents();

            updateStudentCounters();

            showToast(
                "تم حذف جميع الطلاب"
            );

        }
    );

}


/* =====================================================
   التشغيل
===================================================== */

document.addEventListener(
    "DOMContentLoaded",
    () => {

        protectAdminPage();

        initNavigation();

        updateStudentCounters();

        initStudentRegistration();

        initOwnerLogin();

        initLogout();

        renderStudents();

        initStudentSearch();

        initClearStudents();

        initEditForm();

        initModal();

    }
);
