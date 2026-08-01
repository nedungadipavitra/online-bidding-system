
function ButtonOutlined({ color, logo, text }) {
  return (
    <button
      style={{ border: `2px solid ${color}` }}
      className={`rounded-1 px-3 py-2 mx-1 d-flex justify-content-center w-100`}
    >
      {logo ? (
        <img src={logo} alt="" style={{ height: "25px" }} className="mx-2" />
      ) : (
        <></>
      )}
      <div
        style={{
          color: `${color}`,
        }}
        className="mx-2 fw-bold"
      >
        {text}
      </div>
    </button>
  );
}

export default ButtonOutlined;
