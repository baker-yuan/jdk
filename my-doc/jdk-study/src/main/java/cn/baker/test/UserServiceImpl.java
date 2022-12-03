package cn.baker.test;

public class UserServiceImpl implements UserService {
	@Override
	public void test(boolean throwException) {
		System.out.println("...test...");
		if (throwException) {
			throw new IllegalArgumentException("代码执行抛出异常");
		}
	}
}
