package com.thelogicmaster.clearwing.translator;

public interface SignatureSet
{
	public boolean containsSignature(SignatureSet sig);
	public String getSignature();
	public String getMethodName();
	// next signature in the set is null by default.
	public SignatureSet nextSignature();
}
