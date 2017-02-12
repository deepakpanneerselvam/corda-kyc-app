package com.biksen.kyc.model;

import java.util.Date;

public class KYC {
	
	private int kycId;
	private String userId;
	private String userName;
	//"YYYY-MM-DD"
	private Date kycDate;
	private Date kycValidDate;
	private String docId;
	
	public int getKycId() {
		return kycId;
	}
	public void setKycId(int kycId) {
		this.kycId = kycId;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public Date getKycDate() {
		return kycDate;
	}
	public void setKycDate(Date kycDate) {
		this.kycDate = kycDate;
	}
	public Date getKycValidDate() {
		return kycValidDate;
	}
	public void setKycValidDate(Date kycValidDate) {
		this.kycValidDate = kycValidDate;
	}
	public String getDocId() {
		return docId;
	}
	public void setDocId(String docId) {
		this.docId = docId;
	}
	@Override
	public String toString() {
		return "KYC [kycId=" + kycId + ", userId=" + userId + ", userName="
				+ userName + ", kycDate=" + kycDate + ", kycValidDate="
				+ kycValidDate + ", docId=" + docId + "]";
	}
	

}

