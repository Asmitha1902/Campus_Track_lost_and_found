import React, { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import "./Login.css";

const CampusAuth = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const [isRegister, setIsRegister] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [otp, setOtp] = useState("");
  const [showOtpBox, setShowOtpBox] = useState(false);
  const [forgotMode, setForgotMode] = useState(false);
  const [showForgotInput, setShowForgotInput] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const [formData, setFormData] = useState({
    fullName: "",
    studentId: "",
    email: "",
    password: ""
  });

  useEffect(() => {
    if (location.state?.mode === "register") setIsRegister(true);
    else setIsRegister(false);
  }, [location.state]);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const isValidCollegeEmail = (email) =>
    /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.(ac\.in|edu\.in)$/.test(email);

  // ================= HELPER: SHOW TEMPORARY MESSAGE =================
  const showMessage = (type, msg, duration = 4000) => {
    if (type === "error") setErrorMessage(msg);
    if (type === "success") setSuccessMessage(msg);

    setTimeout(() => {
      if (type === "error") setErrorMessage("");
      if (type === "success") setSuccessMessage("");
    }, duration);
  };

  // ================= REGISTER =================
  const handleRegister = async () => {
    setShowOtpBox(false);

    if (!formData.fullName || !formData.studentId || !formData.email || !formData.password) {
      showMessage("error", "All fields are required");
      return;
    }

    if (!isValidCollegeEmail(formData.email)) {
      showMessage("error", "Only college emails like @mlrit.ac.in are allowed");
      return;
    }

    setIsLoading(true);

    try {
      const res = await fetch("https://campus-track-lost-and-found-5.onrender.com/api/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(formData)
      });

      const text = await res.text();
      setIsLoading(false);

      if (!res.ok) {
        showMessage("error", text || "Registration failed. Please try again.", 6000);
        return;
      }

      // Show success for 8 seconds so user can read and check email
      showMessage("success", "✅ " + text + " — Check your inbox & spam folder!", 8000);
      setShowOtpBox(true);

    } catch {
      setIsLoading(false);
      showMessage("error", "Server not reachable. Please try again.");
    }
  };

  // ================= VERIFY OTP =================
  const verifyOtp = async () => {
    if (!otp) {
      showMessage("error", "Enter OTP");
      return;
    }

    try {
      const res = await fetch("https://campus-track-lost-and-found-5.onrender.com/api/auth/verify", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: formData.email, otp })
      });

      const message = await res.text();

      if (!res.ok) {
        showMessage("error", message);
        return;
      }

      showMessage("success", message);
      setShowOtpBox(false);
      setIsRegister(false);
    } catch {
      showMessage("error", "Server error");
    }
  };

  // ================= LOGIN =================
  const handleLogin = async () => {

    if (!formData.email || !formData.password) {
      showMessage("error", "Enter email and password");
      return;
    }

    setIsLoading(true);

    try {
      const res = await fetch("https://campus-track-lost-and-found-5.onrender.com/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email: formData.email,
          password: formData.password
        }),
        credentials: "include"
      });

      const data = await res.json();

      if (!res.ok) {
        showMessage("error", data.message || "Login failed");
        setIsLoading(false);
        return;
      }

      showMessage("success", "Login successful! Redirecting...");

      // Wait 1.5s so the JWT cookie is properly set before navigating
      setTimeout(() => navigate("/dashboard"), 1500);

    } catch {
      showMessage("error", "Server not reachable. Please try again.");
      setIsLoading(false);
    }
  };
  // ================= FORGOT PASSWORD =================
  // Step 1: Show forgot password email input
  const handleShowForgot = () => {
    setShowForgotInput(true);
    setShowOtpBox(false);
    setForgotMode(false);
    setOtp("");
    setErrorMessage("");
    setSuccessMessage("");
  };

  // Step 2: Send OTP to email
  const handleForgotPassword = async () => {
    if (!formData.email) {
      showMessage("error", "Enter your registered email");
      return;
    }

    setIsLoading(true);
    try {
      const res = await fetch("https://campus-track-lost-and-found-5.onrender.com/api/auth/forgot-password", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: formData.email })
      });

      const data = await res.json();
      setIsLoading(false);

      if (!res.ok) {
        showMessage("error", data.message || "Failed to send OTP");
        return;
      }

      setForgotMode(true);
      setShowOtpBox(true);
      showMessage("success", "Reset OTP sent to your email");
    } catch {
      setIsLoading(false);
      showMessage("error", "Server error");
    }
  };

  const resetPassword = async () => {
    if (!otp || !formData.password) {
      showMessage("error", "Enter OTP and new password");
      return;
    }

    try {
      const res = await fetch("https://campus-track-lost-and-found-5.onrender.com/api/auth/reset-password", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: formData.email, otp, password: formData.password })
      });

      const data = await res.json();

      if (!res.ok) {
        showMessage("error", data.message);
        return;
      }

      setForgotMode(false);
      setShowOtpBox(false);
      showMessage("success", "Password Updated Successfully ✅");
    } catch {
      showMessage("error", "Server error");
    }
  };

  const handleGoogleLogin = () => {
    window.location.href = "https://campus-track-lost-and-found-5.onrender.com/oauth2/authorization/google";
  };

  const handleBackToLogin = () => {
    setShowForgotInput(false);
    setShowOtpBox(false);
    setForgotMode(false);
    setOtp("");
    setErrorMessage("");
    setSuccessMessage("");
    setFormData({ fullName: "", studentId: "", email: "", password: "" });
  };

  return (
    <div className="auth-wrapper">
      <button className="back-btn" onClick={() => navigate("/")}>← Back to Home</button>

      <div className="auth-card">
        <div className="auth-left campus">
          <div className="left-content">
            <div className="icon">🎓</div>
            <h1>Campus Lost &amp; Found</h1>
            <p>Securely report, search and recover lost belongings within your university campus.</p>
            <div className="feature">✓ Report Lost Items</div>
            <div className="feature">✓ Post Found Items</div>
            <div className="feature">✓ Secure Student Verification</div>
          </div>
        </div>

        <div className="auth-right">
          <div className="form-container">

            {errorMessage && <p style={{ color: "red", marginBottom: "10px" }}>{errorMessage}</p>}
            {successMessage && <p style={{ color: "green", marginBottom: "10px" }}>{successMessage}</p>}

            {/* ========== FORGOT PASSWORD FLOW ========== */}
            {showForgotInput ? (
              <>
                <h2>Reset Password</h2>
                <p className="sub-text">Enter your registered college email</p>

                <div className="input-group">
                  <input
                    type="email"
                    name="email"
                    required
                    value={formData.email}
                    onChange={handleChange}
                  />
                  <label>College Email</label>
                </div>

                {!showOtpBox && (
                  <button
                    className="primary-btn campus"
                    onClick={handleForgotPassword}
                    disabled={isLoading}
                  >
                    {isLoading ? "Sending OTP..." : "Send Reset OTP"}
                  </button>
                )}

                {showOtpBox && (
                  <>
                    <div className="input-group" style={{ marginTop: "12px" }}>
                      <input
                        type="text"
                        value={otp}
                        onChange={(e) => setOtp(e.target.value)}
                      />
                      <label>Enter OTP</label>
                    </div>

                    <div className="input-group">
                      <input
                        type="password"
                        name="password"
                        required
                        value={formData.password}
                        onChange={handleChange}
                      />
                      <label>New Password</label>
                    </div>

                    <button className="primary-btn campus" onClick={resetPassword}>
                      Reset Password
                    </button>
                  </>
                )}

                <p
                  style={{ textAlign: "center", cursor: "pointer", fontSize: "14px", marginTop: "12px", color: "#4f46e5" }}
                  onClick={handleBackToLogin}
                >
                  ← Back to Login
                </p>
              </>

            ) : !isRegister ? (
              /* ========== LOGIN FORM ========== */
              <>
                <h2>Welcome Back</h2>
                <p className="sub-text">Sign in to your account</p>

                <div className="input-group">
                  <input type="email" name="email" required onChange={handleChange} />
                  <label>College Email</label>
                </div>

                <div className="input-group">
                  <input type="password" name="password" required onChange={handleChange} />
                  <label>Password</label>
                </div>

                <p
                  style={{ textAlign: "right", cursor: "pointer", fontSize: "14px", color: "#4f46e5" }}
                  onClick={handleShowForgot}
                >
                  Forgot Password?
                </p>

                <button
                  className="primary-btn campus"
                  onClick={handleLogin}
                  disabled={isLoading}
                >
                  {isLoading ? "Signing In..." : "Sign In"}
                </button>

                <div className="divider"><span>Or continue with</span></div>

                <button className="google-btn" onClick={handleGoogleLogin}>
                  <img src="https://www.svgrepo.com/show/475656/google-color.svg" alt="google" />
                  Continue with Google
                </button>

                <p className="bottom-text campus">
                  Don't have an account?{" "}
                  <span onClick={() => setIsRegister(true)}>Create Account</span>
                </p>

                {showOtpBox && (
                  <>
                    <div className="input-group" style={{ marginTop: "12px" }}>
                      <input type="text" onChange={(e) => setOtp(e.target.value)} />
                      <label>Enter OTP</label>
                    </div>
                    <button className="primary-btn campus" onClick={verifyOtp}>
                      Verify OTP
                    </button>
                  </>
                )}
              </>

            ) : (
              /* ========== REGISTER FORM ========== */
              <>
                <h2>Create Account</h2>
                <p className="sub-text">Register as a new student</p>

                <div className="double-row">
                  <div className="input-group">
                    <input type="text" name="fullName" required onChange={handleChange} />
                    <label>Full Name</label>
                  </div>

                  <div className="input-group">
                    <input type="text" name="studentId" required onChange={handleChange} />
                    <label>Student ID</label>
                  </div>
                </div>

                <div className="input-group">
                  <input type="email" name="email" required onChange={handleChange} />
                  <label>College Email</label>
                </div>

                <div className="input-group">
                  <input type="password" name="password" required onChange={handleChange} />
                  <label>Password</label>
                </div>

                <button
                  className="primary-btn campus"
                  onClick={handleRegister}
                  disabled={isLoading}
                >
                  {isLoading ? "Sending OTP..." : "Create Account"}
                </button>

                <div className="divider"><span>Or continue with</span></div>

                <button className="google-btn" onClick={handleGoogleLogin}>
                  <img src="https://www.svgrepo.com/show/475656/google-color.svg" alt="google" />
                  Continue with Google
                </button>

                <p className="bottom-text campus">
                  Already have an account? <span onClick={() => setIsRegister(false)}>Sign In</span>
                </p>

                {showOtpBox && (
                  <>
                    <div className="input-group" style={{ marginTop: "12px" }}>
                      <input type="text" onChange={(e) => setOtp(e.target.value)} />
                      <label>Enter OTP</label>
                    </div>
                    <button className="primary-btn campus" onClick={verifyOtp}>
                      Verify OTP
                    </button>
                  </>
                )}
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default CampusAuth;
