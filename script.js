/* =========================================================
   أكاديمية المنى
   Main JavaScript
   ========================================================= */


/* =========================================================
   إعدادات الأكاديمية
   ========================================================= */

const ACADEMY_CONFIG = {

    name: "أكاديمية المنى",

    developer: "yaseen amr abd el rahem",

    studentsKey: "almena_students",

    ownerSessionKey: "almena_owner_session",

    whatsappEtisalat: "201140752330",

    whatsappVodafone: "201061240956"

};


/* =========================================================
   أدوات مساعدة
   ========================================================= */

const $ = (selector) =>
    document.querySelector(selector);

const $$ = (selector) =>
    [...document.querySelectorAll(selector)];


/* =========================================================
   Toast
   ========================================================= */

function showToast(message) {

    let toast = document.querySelector(".toast");

    if (!toast) {

        toast = document.createElement("div");

        toast.className = "toast";

        document.body.appendChild(toast);

    }

    toast.textContent = message;

    toast.classList.add("show");

    clearTimeout(window.__academyToastTimer);

    window.__academyToastTimer =
        setTimeout(() => {

            toast.classList.remove("show");

        }, 2800);

}


/* =========================================================
   القائمة الرئيسية
   ========================================================= */

function initNavToggle() {

    const toggle =
        document.querySelector(".nav-toggle");

    const nav =
        document.querySelector(".main-nav");


    if (!toggle || !nav)
        return;


    toggle.addEventListener("click", () => {

        nav.classList.toggle("open");

    });


    $$(".main-nav a").forEach(link => {

        link.addEventListener("click", () => {

            nav.classList.remove("open");

        });

    });

}


/* =========================================================
   الطلاب
   ========================================================= */

function getStudents() {

    try {

        return JSON.parse(
            localStorage.getItem(
                ACADEMY_CONFIG.studentsKey
            )
        ) || [];

    } catch (error) {

        console.error(
            "خطأ في قراءة بيانات الطلاب:",
            error
        );

        return [];

    }

}


function saveStudents(students) {

    localStorage.setItem(
        ACADEMY_CONFIG.studentsKey,
        JSON.stringify(students)
    );

}


/* =========================================================
   توليد رقم طالب
   ========================================================= */

function generateStudentId() {

    return "ST-" +
        Date.now().toString().slice(-8);

}


/* =========================================================
   تسجيل طالب جديد
   ========================================================= */

function registerStudent(studentData) {

    const students =
        getStudents();


    const student = {

        id: generateStudentId(),

        name: studentData.name || "",

        phone: studentData.phone || "",

        additionalPhone:
            studentData.additionalPhone || "",

        address:
            studentData.address || "",

        age:
            studentData.age || "",

        grade:
            studentData.grade || "",

        parentName:
            studentData.parentName || "",

        parentPhone:
            studentData.parentPhone || "",

        notes:
            studentData.notes || "",

        registrationDate:
            new Date().toISOString()

    };


    students.push(student);

    saveStudents(students);

    return student;

}


/* =========================================================
   تحديث بيانات طالب
   ========================================================= */

function updateStudent(id, updatedData) {

    const students =
        getStudents();


    const index =
        students.findIndex(
            student => student.id === id
        );


    if (index === -1)
        return false;


    students[index] = {

        ...students[index],

        ...updatedData,

        updatedAt:
            new Date().toISOString()

    };


    saveStudents(students);

    return true;

}


/* =========================================================
   حذف طالب
   ========================================================= */

function deleteStudent(id) {

    const students =
        getStudents();


    const filtered =
        students.filter(
            student => student.id !== id
        );


    saveStudents(filtered);

    return true;

}


/* =========================================================
   عدد الطلاب
   ========================================================= */

function getStudentCount() {

    return getStudents().length;

}


function updateStudentCounters() {

    const count =
        getStudentCount();


    $$("[data-student-count]").forEach(el => {

        el.textContent =
            count.toLocaleString("ar-EG");

    });

}


/* =========================================================
   التحقق من البريد الإلكتروني
   ========================================================= */

function isValidEmail(email) {

    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/
        .test(email);

}


/* =========================================================
   التحقق من الحقول
   ========================================================= */

function validateField(
    field,
    condition,
    message
) {

    if (!field)
        return false;


    const error =
        field.querySelector(".field-error");


    if (!condition) {

        field.classList.add("invalid");

        if (error)
            error.textContent = message;

        return false;

    }


    field.classList.remove("invalid");

    return true;

}


/* =========================================================
   نموذج تسجيل الطالب
   ========================================================= */

function initStudentRegistration() {

    const form =
        document.getElementById(
            "student-register-form"
        );


    if (!form)
        return;


    form.addEventListener(
        "submit",
        function(event) {

            event.preventDefault();


            const formData =
                new FormData(form);


            const student = {

                name:
                    String(
                        formData.get("name") || ""
                    ).trim(),

                phone:
                    String(
                        formData.get("phone") || ""
                    ).trim(),

                additionalPhone:
                    String(
                        formData.get(
                            "additionalPhone"
                        ) || ""
                    ).trim(),

                address:
                    String(
                        formData.get("address") || ""
                    ).trim(),

                age:
                    String(
                        formData.get("age") || ""
                    ).trim(),

                grade:
                    String(
                        formData.get("grade") || ""
                    ).trim(),

                parentName:
                    String(
                        formData.get(
                            "parentName"
                        ) || ""
                    ).trim(),

                parentPhone:
                    String(
                        formData.get(
                            "parentPhone"
                        ) || ""
                    ).trim(),

                notes:
                    String(
                        formData.get("notes") || ""
                    ).trim()

            };


            /* =========================
               التحقق
            ========================= */

            if (student.name.length < 3) {

                showToast(
                    "يرجى كتابة اسم الطالب بشكل صحيح"
                );

                return;

            }


            if (student.phone.length < 8) {

                showToast(
                    "يرجى كتابة رقم الهاتف بشكل صحيح"
                );

                return;

            }


            if (!student.address) {

                showToast(
                    "يرجى كتابة مكان السكن"
                );

                return;

            }


            /* =========================
               حفظ الطالب
            ========================= */

            const savedStudent =
                registerStudent(student);


            /* =========================
               رسالة واتساب
            ========================= */

            sendStudentToWhatsApp(
                savedStudent
            );


            showToast(
                "تم تسجيل الطالب بنجاح ✓"
            );


            form.reset();


            updateStudentCounters();

        }

    );

}


/* =========================================================
   إنشاء رسالة الطالب للواتساب
   ========================================================= */

function createStudentWhatsAppMessage(student) {

    let message =

        `🎓 *تسجيل طالب جديد - ${ACADEMY_CONFIG.name}*%0A` +
        `%0A` +

        `👤 *اسم الطالب:* ${student.name}%0A` +

        `📞 *رقم الهاتف:* ${student.phone}%0A` +

        `📱 *رقم إضافي:* ${
            student.additionalPhone || "غير موجود"
        }%0A` +

        `📍 *العنوان:* ${student.address}%0A` +

        `🎂 *العمر:* ${
            student.age || "غير محدد"
        }%0A` +

        `📚 *الصف:* ${
            student.grade || "غير محدد"
        }%0A` +

        `👨‍👩‍👦 *اسم ولي الأمر:* ${
            student.parentName || "غير محدد"
        }%0A` +

        `☎️ *رقم ولي الأمر:* ${
            student.parentPhone || "غير محدد"
        }%0A` +

        `📝 *ملاحظات:* ${
            student.notes || "لا توجد"
        }%0A` +

        `%0A` +

        `🆔 *رقم التسجيل:* ${student.id}%0A` +

        `📅 *تاريخ التسجيل:* ${
            new Date(
                student.registrationDate
            ).toLocaleString("ar-EG")
        }`;


    return message;

}


/* =========================================================
   إرسال التسجيل عبر واتساب
   ========================================================= */

function sendStudentToWhatsApp(student) {

    const message =
        createStudentWhatsAppMessage(student);


    /*
       نرسل الطلب إلى رقم اتصالات
       ويمكن تغييره إلى فودافون بسهولة.
    */

    const phone =
        ACADEMY_CONFIG.whatsappEtisalat;


    const url =
        `https://wa.me/${phone}?text=${message}`;


    window.open(
        url,
        "_blank",
        "noopener,noreferrer"
    );

}


/* =========================================================
   إرسال إلى رقم فودافون
   ========================================================= */

function sendStudentToVodafone(student) {

    const message =
        createStudentWhatsAppMessage(student);


    const phone =
        ACADEMY_CONFIG.whatsappVodafone;


    const url =
        `https://wa.me/${phone}?text=${message}`;


    window.open(
        url,
        "_blank",
        "noopener,noreferrer"
    );

}


/* =========================================================
   جلسة المالك
   ========================================================= */

function getOwnerSession() {

    try {

        const localSession =
            localStorage.getItem(
                ACADEMY_CONFIG.ownerSessionKey
            );


        if (localSession)
            return JSON.parse(localSession);


        const session =
            sessionStorage.getItem(
                ACADEMY_CONFIG.ownerSessionKey
            );


        if (session)
            return JSON.parse(session);


        return null;

    } catch (error) {

        return null;

    }

}


/* =========================================================
   التحقق من دخول المالك
   ========================================================= */

function isOwnerLoggedIn() {

    const session =
        getOwnerSession();


    return !!(
        session &&
        session.loggedIn === true
    );

}


/* =========================================================
   حماية لوحة الإدارة
   ========================================================= */

function protectAdminPage() {

    const isAdminPage =
        document.body.classList.contains(
            "admin-page"
        );


    const isAdminFile =
        window.location.pathname
            .toLowerCase()
            .includes("admin.html");


    if (
        (isAdminPage || isAdminFile) &&
        !isOwnerLoggedIn()
    ) {

        window.location.href =
            "login.html";

    }

}


/* =========================================================
   تسجيل خروج المالك
   ========================================================= */

function ownerLogout() {

    localStorage.removeItem(
        ACADEMY_CONFIG.ownerSessionKey
    );


    sessionStorage.removeItem(
        ACADEMY_CONFIG.ownerSessionKey
    );


    window.location.href =
        "login.html";

}


/* =========================================================
   زر تسجيل الخروج
   ========================================================= */

function initLogout() {

    const buttons =
        $$("[data-owner-logout]");


    buttons.forEach(button => {

        button.addEventListener(
            "click",
            function(event) {

                event.preventDefault();

                ownerLogout();

            }
        );

    });

}


/* =========================================================
   عرض بيانات المالك
   ========================================================= */

function updateOwnerInfo() {

    const session =
        getOwnerSession();


    if (!session)
        return;


    $$("[data-owner-name]").forEach(el => {

        el.textContent =
            session.username || "المالك";

    });

}


/* =========================================================
   إحصائيات لوحة الإدارة
   ========================================================= */

function updateAdminStats() {

    const students =
        getStudents();


    $$("[data-total-students]").forEach(el => {

        el.textContent =
            students.length
                .toLocaleString("ar-EG");

    });


    $$("[data-today-students]").forEach(el => {

        const today =
            new Date()
                .toISOString()
                .split("T")[0];


        const count =
            students.filter(student => {

                return student.registrationDate &&
                    student.registrationDate
                        .startsWith(today);

            }).length;


        el.textContent =
            count.toLocaleString("ar-EG");

    });

}


/* =========================================================
   البحث عن الطلاب
   ========================================================= */

function searchStudents(query) {

    const students =
        getStudents();


    const value =
        String(query || "")
            .trim()
            .toLowerCase();


    if (!value)
        return students;


    return students.filter(student => {

        return (

            String(student.name)
                .toLowerCase()
                .includes(value)

            ||

            String(student.phone)
                .toLowerCase()
                .includes(value)

            ||

            String(student.id)
                .toLowerCase()
                .includes(value)

        );

    });

}


/* =========================================================
   عرض الطلاب داخل الجدول
   ========================================================= */

function renderStudentsTable(
    container,
    students = getStudents()
) {

    if (!container)
        return;


    if (!students.length) {

        container.innerHTML = `

            <div class="empty-state">

                <div class="empty-icon">
                    👨‍🎓
                </div>

                <h3>
                    لا يوجد طلاب حتى الآن
                </h3>

                <p>
                    عند تسجيل طالب جديد
                    سيظهر هنا.
                </p>

            </div>

        `;

        return;

    }


    container.innerHTML = students
        .map(student => `

            <div
                class="student-row"
                data-student-id="${student.id}">

                <div>
                    <strong>
                        ${escapeHTML(student.name)}
                    </strong>

                    <small>
                        ${escapeHTML(student.id)}
                    </small>
                </div>


                <div>
                    ${escapeHTML(student.phone)}
                </div>


                <div>
                    ${escapeHTML(
                        student.grade || "-"
                    )}
                </div>


                <div>
                    ${escapeHTML(
                        student.address || "-"
                    )}
                </div>


                <div class="student-actions">

                    <button
                        type="button"
                        data-edit-student="${student.id}">

                        تعديل

                    </button>


                    <button
                        type="button"
                        data-delete-student="${student.id}">

                        حذف

                    </button>

                </div>

            </div>

        `)
        .join("");


    initStudentTableActions();

}


/* =========================================================
   حماية النصوص
   ========================================================= */

function escapeHTML(value) {

    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");

}


/* =========================================================
   أزرار جدول الطلاب
   ========================================================= */

function initStudentTableActions() {

    $$("[data-delete-student]")
        .forEach(button => {

            button.addEventListener(
                "click",
                function() {

                    const id =
                        this.dataset.deleteStudent;


                    const confirmed =
                        confirm(
                            "هل تريد حذف هذا الطالب؟"
                        );


                    if (!confirmed)
                        return;


                    deleteStudent(id);

                    renderStudentsTable(
                        document.querySelector(
                            "[data-students-table]"
                        )
                    );


                    updateAdminStats();

                    updateStudentCounters();


                    showToast(
                        "تم حذف الطالب"
                    );

                }
            );

        });


    $$("[data-edit-student]")
        .forEach(button => {

            button.addEventListener(
                "click",
                function() {

                    const id =
                        this.dataset.editStudent;


                    const student =
                        getStudents().find(
                            item => item.id === id
                        );


                    if (!student)
                        return;


                    openStudentEdit(student);

                }
            );

        });

}


/* =========================================================
   تعديل الطالب
   ========================================================= */

function openStudentEdit(student) {

    const name =
        prompt(
            "اسم الطالب:",
            student.name
        );


    if (name === null)
        return;


    const phone =
        prompt(
            "رقم الهاتف:",
            student.phone
        );


    if (phone === null)
        return;


    const address =
        prompt(
            "العنوان:",
            student.address
        );


    if (address === null)
        return;


    updateStudent(
        student.id,
        {

            name: name.trim(),

            phone: phone.trim(),

            address: address.trim()

        }
    );


    renderStudentsTable(
        document.querySelector(
            "[data-students-table]"
        )
    );


    updateAdminStats();

    updateStudentCounters();


    showToast(
        "تم تعديل بيانات الطالب ✓"
    );

}


/* =========================================================
   البحث في لوحة الإدارة
   ========================================================= */

function initStudentSearch() {

    const input =
        document.querySelector(
            "[data-student-search]"
        );


    const table =
        document.querySelector(
            "[data-students-table]"
        );


    if (!input || !table)
        return;


    input.addEventListener(
        "input",
        function() {

            const results =
                searchStudents(
                    this.value
                );


            renderStudentsTable(
                table,
                results
            );

        }
    );

}


/* =========================================================
   تشغيل النظام
   ========================================================= */

document.addEventListener(
    "DOMContentLoaded",
    function() {

        initNavToggle();

        initStudentRegistration();

        initLogout();

        updateStudentCounters();

        updateAdminStats();

        updateOwnerInfo();

        initStudentSearch();

        protectAdminPage();


        const studentsTable =
            document.querySelector(
                "[data-students-table]"
            );


        if (studentsTable) {

            renderStudentsTable(
                studentsTable
            );

        }

    }
);
