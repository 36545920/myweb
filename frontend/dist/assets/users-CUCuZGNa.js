import{c as s}from"./client-wE0PFglW.js";const t={getProfile:()=>s.get("/users/me"),updateProfile:e=>s.put("/users/me",e),search:e=>s.get("/users/search",{params:{email:e}})};export{t as u};
