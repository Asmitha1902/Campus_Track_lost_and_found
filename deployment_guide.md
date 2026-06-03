# Campus Tracker - Step-by-Step Deployment Guide

Follow these instructions strictly in order to deploy your application to the internet for free.

---

## STEP 1: Push Code to GitHub
Before deploying anything, your code must be on GitHub.
1. Go to [GitHub](https://github.com/) and create **two** separate repositories:
   * `campus-tracker-frontend`
   * `campus-tracker-backend`
2. Open your terminal, navigate to your `frontend` folder, and push the code to the frontend repository.
3. Navigate to your `backend` folder and push the code to the backend repository.

---

## STEP 2: Deploy Database (MySQL) on Aiven
Your application needs a cloud database to store data.
1. Go to [Aiven.io](https://aiven.io/) and create a free account.
2. Click **Create Service** -> Select **MySQL**.
3. Choose the **Free Plan** and click Create.
4. Once created, Aiven will give you the **Connection Details** (Host URL, Port, User, Password). **Save these details.**

---

## STEP 3: Update & Deploy Backend (Spring Boot) on Render
Render will host your Java Spring Boot API.

### 1. Update Backend Code
Before deploying, you must connect the backend to your new cloud database.
* Open `backend/src/main/resources/application.properties`.
* Update the database lines using the Aiven details from Step 2:
  ```properties
  spring.datasource.url=jdbc:mysql://[AIVEN_HOST_URL]:[AIVEN_PORT]/defaultdb?sslMode=REQUIRED
  spring.datasource.username=[AIVEN_USER]
  spring.datasource.password=[AIVEN_PASSWORD]
  ```
* Push these changes to your GitHub backend repository.

### 2. Deploy on Render
1. Go to [Render.com](https://render.com/) and create a free account.
2. Click **New +** and select **Web Service**.
3. Connect your GitHub account and select your `campus-tracker-backend` repository.
4. Fill in the settings:
   * **Runtime:** Java
   * **Build Command:** `mvn clean package -DskipTests`
   * **Start Command:** `java -jar target/portal-0.0.1-SNAPSHOT.jar`
5. Click **Create Web Service**.
6. Once the build is finished, Render will give you a live URL (e.g., `https://campus-backend.onrender.com`). **Save this URL.**

---

## STEP 4: Update & Deploy Frontend (React) on Vercel
Vercel will host your React website.

### 1. Update Frontend Code
Currently, your frontend points to `localhost`. You need to point it to Render.
* Open your frontend code in VS Code.
* Search globally for `http://localhost:9090`.
* Replace every single instance of `http://localhost:9090` with your new Render Backend URL (e.g., `https://campus-backend.onrender.com`).
* Push these changes to your GitHub frontend repository.

### 2. Deploy on Vercel
1. Go to [Vercel.com](https://vercel.com/) and create a free account using GitHub.
2. Click **Add New -> Project**.
3. Select your `campus-tracker-frontend` repository from the list and click **Import**.
4. Vercel will automatically detect that it is a Vite/React project.
5. Click **Deploy**.
6. Within 2 minutes, Vercel will give you the final live URL for your website (e.g., `https://campus-tracker.vercel.app`).

---

## STEP 5: Final CORS Update
Your backend needs permission to accept requests from your new Vercel website.
1. Go back to your backend code in VS Code.
2. Open files like `ItemController.java`, `AuthController.java`, etc.
3. Find the line: `@CrossOrigin(origins = "http://localhost:5173")`
4. Change it to your new Vercel URL: `@CrossOrigin(origins = "https://campus-tracker.vercel.app")`
5. Push the code to GitHub. Render will automatically detect the push and restart your backend with the new settings.

### 🎉 **You are done! Your app is now live on the internet!**
